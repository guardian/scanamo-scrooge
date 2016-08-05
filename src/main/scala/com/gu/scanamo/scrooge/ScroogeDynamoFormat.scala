package com.gu.scanamo.scrooge

import cats.NotNull
import cats.data.Xor
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.{DynamoReadError, NoPropertyOfType, TypeCoercionError}
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftUnion}

import scala.language.experimental.macros
import scala.reflect.ClassTag

object ScroogeDynamoFormat extends LowerPriorityImplicits {
  implicit def seqFormat[T](implicit f: DynamoFormat[T]): DynamoFormat[Seq[T]] =
    DynamoFormat.xmap[Seq[T], List[T]](l => Xor.right(l.toSeq))(_.toList)

  implicit def scroogeScanamoEnumFormat[T <: ThriftEnum]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.enumMacro[T]
  implicit def scroogeScanamoUnionFormat[T <: ThriftUnion]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.unionMacro[T]


  def attribute[T](
    decode: AttributeValue => T, propertyType: String)(
    encode: AttributeValue => T => AttributeValue
  ): DynamoFormat[T] = {
    new DynamoFormat[T] {
      override def read(av: AttributeValue): Xor[DynamoReadError, T] =
        Xor.fromOption(Option(decode(av)), NoPropertyOfType(propertyType, av))
      override def write(t: T): AttributeValue =
        encode(new AttributeValue())(t)
    }
  }
  val numFormat = attribute(_.getN, "N")(_.withN)
  def coerce[A, B, T >: scala.Null <: scala.Throwable](f: A => B)(implicit T: ClassTag[T], NT: NotNull[T]): A => Xor[DynamoReadError, B] = a =>
    Xor.catchOnly[T](f(a)).leftMap(TypeCoercionError(_))
  def coerceNumber[N](f: String => N): String => Xor[DynamoReadError, N] =
    coerce[String, N, NumberFormatException](f)

  implicit val shortFormat = DynamoFormat.xmap(coerceNumber(_.toShort))(_.toString)(numFormat)
}

trait LowerPriorityImplicits {
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

  private val UNION_DISCRIMINATOR_FIELD_NAME = "SCANAMO_SCROOGE_UNION_DISCRIMINATOR"
  def unionMacro[T: c.WeakTypeTag]: Tree = {
    val A = weakTypeOf[T].dealias
    if (A.typeSymbol.isAbstract) {
      val subClasses = A.typeSymbol.asClass.knownDirectSubclasses
      val cases = subClasses map { cl =>
        val typ = tq"${cl.asType}"
        val pat = pq"""x : $typ"""
        if (cl.name.toString == "UnknownUnionField")
          (
            Option.empty[c.universe.Tree],
            cq"""$pat => throw new RuntimeException("Can't serialise UnknownUnionField")"""
          )
        else {
          val format = implicitFormatFor(cl.asType.toType)
          (
            Some(cq"""${cl.asType.toType.toString} => $format.read(av)"""),
            cq"""$pat => {
                  val av = ${format}.write(x)
                  val m = new java.util.HashMap[String, com.amazonaws.services.dynamodbv2.model.AttributeValue]()
                  m.putAll(av.getM)
                  m.put($UNION_DISCRIMINATOR_FIELD_NAME, new com.amazonaws.services.dynamodbv2.model.AttributeValue().withS(${cl.asType.toType.toString}))
                  new com.amazonaws.services.dynamodbv2.model.AttributeValue().withM(m)
               }
            """
          )
        }
      }
      val (readCases, writeCases) = cases.unzip

      val res = q"""
       new DynamoFormat[$A] {
         def read(av: AttributeValue) = cats.data.Xor.fromOption[com.gu.scanamo.error.DynamoReadError, java.util.Map[String, com.amazonaws.services.dynamodbv2.model.AttributeValue]](
           Option(av.getM), com.gu.scanamo.error.NoPropertyOfType("M", av))
           .ensure(com.gu.scanamo.error.InvalidPropertiesError(cats.data.NonEmptyList(com.gu.scanamo.error.PropertyReadError($UNION_DISCRIMINATOR_FIELD_NAME, com.gu.scanamo.error.MissingProperty))))(
             map => map.get($UNION_DISCRIMINATOR_FIELD_NAME) != null && map.get($UNION_DISCRIMINATOR_FIELD_NAME).getS != null
           ).flatMap(_.get($UNION_DISCRIMINATOR_FIELD_NAME).getS match {
             case ..${readCases.flatten}
             case unknown => Xor.left(
               com.gu.scanamo.error.InvalidPropertiesError(cats.data.NonEmptyList(
                 com.gu.scanamo.error.PropertyReadError($UNION_DISCRIMINATOR_FIELD_NAME,
                   com.gu.scanamo.error.TypeCoercionError(new RuntimeException(unknown + " is not a known subtype of " + ${A.toString}))
                 )
               ))
             )
           })
         def write(a: $A): AttributeValue = {
          a match { case ..${writeCases} }
         }
       }
     """
      println(res)
      res
    } else structMacro[T]
  }

  def structMacro[T: c.WeakTypeTag]: Tree = {
    val A = weakTypeOf[T].dealias

    val apply = A.companion.member(TermName("apply")) match {
      case symbol if symbol.isMethod && symbol.asMethod.paramLists.size == 1 => symbol.asMethod
      case _ => c.abort(c.enclosingPosition, s"$A is not a valid Scrooge class: could not find the companion object's apply method")
    }

    val params = apply.paramLists.head.zipWithIndex.map { case (param, i) =>
      val name = param.name
      val tpe = param.typeSignature
      val fresh = c.freshName(name).toTermName
      val termName = name.toTermName

      val reader =
        q"""
          val $fresh: cats.data.Validated[com.gu.scanamo.error.InvalidPropertiesError, $tpe] = {
            val format = ${implicitFormatFor(tpe)}
            val possibleValue = avMap.get(${termName.toString}).map(format.read).orElse(format.default.map(cats.data.Xor.right))
            val validatedValue = possibleValue.getOrElse(cats.data.Xor.left[com.gu.scanamo.error.DynamoReadError, $tpe](com.gu.scanamo.error.MissingProperty))
            validatedValue.leftMap(e => com.gu.scanamo.error.InvalidPropertiesError(cats.data.NonEmptyList(com.gu.scanamo.error.PropertyReadError(${termName.toString}, e)))).toValidated
          }
          """

      val writer = q"""${termName.toString} -> ${implicitFormatFor(tpe)}.write(t.$termName)"""
      val freshSuccessful = q"""$fresh.toOption.get"""
      (writer, reader, fresh, freshSuccessful)
    }

    val reducedReader = if(params.length != 1)
      params.map(_._3).tail.foldLeft[c.universe.Tree](q"${params.head._3}")((a, b) => q"$a.product($b)")
    else
      q"""${params.head._3}"""

    val f = q"""
      new com.gu.scanamo.DynamoFormat[$A] {
        def read(av: com.amazonaws.services.dynamodbv2.model.AttributeValue): cats.data.Xor[com.gu.scanamo.error.DynamoReadError, $A] = {
          val avMap = collection.convert.WrapAsScala.mapAsScalaMap(av.getM)

          ..${params.map(_._2)}

          val reader = ${reducedReader}

          reader.toXor.map(params =>
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
    f
  }

  def implicitFormatFor(tpe: c.universe.Type) = {
    val formatForType = appliedType(weakTypeOf[DynamoFormat[_]].typeConstructor, tpe)
    val implicitFormat = c.inferImplicitValue(formatForType)
    if (implicitFormat.nonEmpty) {
      implicitFormat
    } else {
      // Note the use of shapeless `Lazy` to work around a problem with diverging implicits.
      // If you try to summon an implicit for heavily nested type, e.g. `DynamoFormat[Option[List[String]]]` then the compiler sometimes gives up.
      // Wrapping with `Lazy` fixes this issue.
      q"""_root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.com.gu.scanamo.DynamoFormat[$tpe]]].value"""
    }
  }
}
