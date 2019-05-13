package org.scanamo.scrooge

import org.scanamo.DynamoFormat
import com.twitter.scrooge.{ThriftEnum, ThriftStruct}

import scala.language.experimental.macros

object ScroogeDynamoFormat {
  implicit def scroogeScanamoEnumFormat[T <: ThriftEnum]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.enumMacro[T]
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
      org.scanamo.DynamoFormat.xmap[$A, String](
        (x) => $valueOf(x).toRight[org.scanamo.error.DynamoReadError](org.scanamo.error.TypeCoercionError(new IllegalArgumentException(x + " is not a valid " + $typeName))))(
        _.name)
    """
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

      val formatForType = appliedType(weakTypeOf[DynamoFormat[_]].typeConstructor, tpe)
      val implicitFormat = c.inferImplicitValue(formatForType)
      val formatWithFallback= if (implicitFormat.nonEmpty) {
        implicitFormat
      } else {
        // Note the use of shapeless `Lazy` to work around a problem with diverging implicits.
        // If you try to summon an implicit for heavily nested type, e.g. `DynamoFormat[Option[List[String]]]` then the compiler sometimes gives up.
        // Wrapping with `Lazy` fixes this issue.
        q"""_root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.org.scanamo.DynamoFormat[$tpe]]].value"""
      }

      val reader =
        q"""
          val $fresh: cats.data.Validated[org.scanamo.error.InvalidPropertiesError, $tpe] = {
            val format = $formatWithFallback
            val possibleValue: Option[Either[org.scanamo.error.DynamoReadError, $tpe]] = collection.convert.WrapAsScala.mapAsScalaMap(av.getM).get(${termName.toString})
              .map[Either[org.scanamo.error.DynamoReadError, $tpe]](format.read)
              .orElse[Either[org.scanamo.error.DynamoReadError, $tpe]](
                format.default.map[Either[org.scanamo.error.DynamoReadError, $tpe]](Right(_))
              )
            val validatedValue: Either[org.scanamo.error.DynamoReadError, $tpe] = possibleValue.getOrElse(Left[org.scanamo.error.DynamoReadError, $tpe](org.scanamo.error.MissingProperty))
            cats.data.Validated.fromEither(validatedValue.left.map(e => org.scanamo.error.InvalidPropertiesError(cats.data.NonEmptyList.of(org.scanamo.error.PropertyReadError(${termName.toString}, e)))))
          }
          """
      val writer = q"""${termName.toString} -> _root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.org.scanamo.DynamoFormat[$tpe]]].value.write(t.$termName)"""
      val freshSuccesful = q"""$fresh.toOption.get"""
      (writer, reader, fresh, freshSuccesful)
    }

    val reducedWriter = if(params.length != 1)
      q"""List[cats.data.Validated[org.scanamo.error.InvalidPropertiesError, Any]](..${params.map(_._3)}).reduce(_.product(_))"""
    else
      q"""${params.head._3}"""

    q"""
      new org.scanamo.DynamoFormat[$A] {
        def read(av: com.amazonaws.services.dynamodbv2.model.AttributeValue): Either[org.scanamo.error.DynamoReadError, $A] = {
          ..${params.map(_._2)}

          ${reducedWriter}.toEither.map(_ =>
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
