package com.gu.scanamo.scrooge

import com.gu.scanamo.DynamoFormat
import com.twitter.scrooge.ThriftEnum

import scala.language.experimental.macros

object ScroogeDynamoFormat {
  implicit def scroogeScanamoEnumFormat[T <: ThriftEnum]: DynamoFormat[T] = macro ScroogeDynamoFormatMacro.enumMacro[T]
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
        (x) => _root_.cats.data.Xor.fromOption($valueOf(x), com.gu.scanamo.error.TypeCoercionError(throw new IllegalArgumentException(x + " is not a valid " + $typeName))))(
        _.name)
    """
  }
}