package com.gu.scanamo.scrooge

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.{ChangeRecord, User, Flags}
import com.gu.contentatom.thrift.atom.media.{ Metadata, AssetType }
import com.gu.scanamo.DynamoFormat
import org.scalatest.{FunSuite, Matchers}

class ScroogeDynamoFormatTest extends FunSuite with Matchers {

  def roundTrip[A : DynamoFormat](a: A) = {
    DynamoFormat[A].read(DynamoFormat[A].write(a)) should be(Right(a))
  }

  test("testScroogeScanamoEnumFormat") {
    val format = ScroogeDynamoFormat.scroogeScanamoEnumFormat[AssetType]
    format.read(format.write(AssetType.Audio)) should be(Right(AssetType.Audio))
    format.read(format.write(AssetType.Video)) should be(Right(AssetType.Video))
    format.read(new AttributeValue().withS("Spleurk")).isLeft should be(true)
  }

  test("testScroogeScanamoStructFormat") {
    import ScroogeDynamoFormat._
    roundTrip(ChangeRecord(1L, Some(User("email", Some("f"), None))))
  }

  test("testScroogeScanamoStructFormat for struct with one member") {
    import ScroogeDynamoFormat._
    roundTrip(Flags(Some(true)))
  }

  test("testScroogeScanamoStructFormat for struct with all optional members") {
    import ScroogeDynamoFormat._
    roundTrip[Metadata](Metadata())
  }

}
