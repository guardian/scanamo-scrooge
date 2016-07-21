package com.gu.scanamo.scrooge

import com.gu.scanamo.DynamoFormat
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftUnion}

import scala.language.experimental.macros

object ScroogeDynamoFormat {
  implicit def scroogeScanamoEnumFormat[T <: ThriftEnum]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.enumMacro[T]
  def scroogeScanamoUnionFormat[T <: ThriftUnion]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.unionMacro[T]
  implicit def scroogeScanamoStructFormat[T <: ThriftStruct]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.structMacro[T]
}

import scala.reflect.macros.blackbox

import macrocompat.bundle

@bundle
class ScroogeDynamoFormatMacro(val c: blackbox.Context) {
  import c.universe._

  def enumMacro[T: c.WeakTypeTag]: Tree = {
    val A = weakTypeOf[T]
    val typeName = A.typeSymbol.name.toString
    val valueOf = A.companion.member(TermName("valueOf"))

    q"""
      com.gu.scanamo.DynamoFormat.xmap[$A, String](
        (x) => _root_.cats.data.Xor.fromOption($valueOf(x), com.gu.scanamo.error.TypeCoercionError(new IllegalArgumentException(x + " is not a valid " + $typeName))))(
        _.name)
    """
  }

  def unionMacro[T: c.WeakTypeTag]: Tree = {
    val A = weakTypeOf[T]
    val subClasses = A.typeSymbol.asClass.knownDirectSubclasses
    val cases = subClasses map { cl =>
      val typ = tq"${cl.asType}"
      val pat = pq"""x @ $typ"""
      if(cl.name.toString == "UnknownUnionField")
        cq"""_: $typ => Xor.left(com.gu.scanamo.error.TypeCoercionError(new IllegalArgumentException("unknown union field")))"""
      else
        cq"""$pat => com.gu.scanamo.DynamoFormat[$typ].write(x)"""
    }
    val res = q"""
     new DynamoFormat[$A] {
       def read(av: AttributeValue) = ???
       def write(a: $A) = a match { case ..${cases} }
     }
     """
    println(showCode(res))
    res
  }

  def structMacro[T: c.WeakTypeTag]: Tree = {
    val A = weakTypeOf[T]

    val apply = A.companion.member(TermName("apply")) match {
      case symbol if symbol.isMethod && symbol.asMethod.paramLists.size == 1 => symbol.asMethod
      case _ => c.abort(c.enclosingPosition, "Not a valid Scrooge class: could not find the companion object's apply method")
    }

    val params = apply.paramLists.head.zipWithIndex.map { case (param, i) =>
      val name = param.name
      val tpe = param.typeSignature
      val fresh = c.freshName(name).toTermName
      val termName = name.toTermName

      // Note the use of shapeless `Lazy` to work around a problem with diverging implicits.
      // If you try to summon an implicit for heavily nested type, e.g. `Decoder[Option[Seq[String]]]` then the compiler sometimes gives up.
      // Wrapping with `Lazy` fixes this issue.
      val reader =
        q"""
          val $fresh: cats.data.Validated[com.gu.scanamo.error.InvalidPropertiesError, $tpe] = {
            val format = _root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.com.gu.scanamo.DynamoFormat[$tpe]]].value
            val possibleValue = collection.convert.WrapAsScala.mapAsScalaMap(av.getM).get(${termName.toString}).map(format.read).orElse(format.default.map(cats.data.Xor.right))
            val validatedValue = possibleValue.getOrElse(cats.data.Xor.left[com.gu.scanamo.error.DynamoReadError, $tpe](com.gu.scanamo.error.MissingProperty))
            validatedValue.leftMap(e => com.gu.scanamo.error.InvalidPropertiesError(cats.data.NonEmptyList(com.gu.scanamo.error.PropertyReadError(${termName.toString}, e)))).toValidated
          }
          """
      val writer = q"""${termName.toString} -> _root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.com.gu.scanamo.DynamoFormat[$tpe]]].value.write(t.$termName)"""
      val freshSuccesful = q"""$fresh.toOption.get"""
      (writer, reader, fresh, freshSuccesful)
    }

    val reducedWriter = if(params.length != 1)
      q"""List(..${params.map(_._3)}).reduce(_.product(_))"""
    else
      q"""${params.head._3}"""

    q"""
      new com.gu.scanamo.DynamoFormat[$A] {
        def read(av: com.amazonaws.services.dynamodbv2.model.AttributeValue): cats.data.Xor[com.gu.scanamo.error.DynamoReadError, $A] = {
          ..${params.map(_._2)}

          ${reducedWriter}.toXor.map(_ =>
            ${apply.asMethod}(
              ..${params.map(_._4)}
            )
          )
        }
        def write(t: $A): com.amazonaws.services.dynamodbv2.model.AttributeValue =
          new com.amazonaws.services.dynamodbv2.model.AttributeValue().withM(collection.convert.WrapAsJava.mapAsJavaMap(
            Seq(..${params.map(_._1)}).foldLeft(Map.empty[String, com.amazonaws.services.dynamodbv2.model.AttributeValue])((m,p) =>
              m + p
            )
          ))
      }
    """
  }
}
