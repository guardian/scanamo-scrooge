package com.gu.scanamo.scrooge

import cats.data.Xor
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.{ChangeRecord, User, Flags, AtomData}
import com.gu.contentatom.thrift.atom.media.{MediaAtom, AssetType, Asset}
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.DynamoFormat._
import com.gu.scanamo.error.TypeCoercionError
import org.scalatest.{FunSuite, Matchers}

class ScroogeDynamoFormatTest extends FunSuite with Matchers {

  test("testScroogeScanamoEnumFormat") {
    val format = ScroogeDynamoFormat.scroogeScanamoEnumFormat[AssetType]
    format.read(format.write(AssetType.Audio)) should be(Xor.right(AssetType.Audio))
    format.read(format.write(AssetType.Video)) should be(Xor.right(AssetType.Video))
    format.read(new AttributeValue().withS("Spleurk")).isLeft should be(true)
  }

  test("testScroogeScanamoStructFormat") {
    import ScroogeDynamoFormat._
    val changeRecord = ChangeRecord(1L, Some(User("email", Some("f"), None)))
    DynamoFormat[ChangeRecord].read(DynamoFormat[ChangeRecord].write(changeRecord)) should be(Xor.right(changeRecord))
  }

  test("testScroogeScanamoStructFormat for struct with one member") {
    import ScroogeDynamoFormat._
    val flags = Flags(Some(true))
    DynamoFormat[Flags].read(DynamoFormat[Flags].write(flags)) should be(Xor.right(flags))
  }

  test("testScroogeScanamoUnionFormat") {
    import ScroogeDynamoFormat._
    val atomData = AtomData.Media(MediaAtom(activeVersion = 1L, assets = List()))
    val fmt = ScroogeDynamoFormat.scroogeScanamoUnionFormat[AtomData]

}
