package com.gu.scanamo.scrooge

import cats.data.Xor
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.{ChangeRecord, User}
import com.gu.contentatom.thrift.atom.media.AssetType
import com.gu.scanamo.DynamoFormat
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
}
