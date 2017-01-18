package com.gu.scanamo.scrooge

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media._
import com.gu.scanamo.DynamoFormat
import org.scalatest.{FunSuite, Matchers}
import cats.syntax.either._

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

  test("testScroogeScanamoStructFormat Media Atom") {
    import ScroogeDynamoFormat._
    val assets = List(Asset(AssetType.Audio, 2L, "SomeId", Platform.Facebook, None), Asset(AssetType.Audio, 2L, "SomeOtherId", Platform.Facebook, None))
    val metadata = Metadata(Some(List("tag1", "tag2")), None, None, Some(true), None, Some(PrivacyStatus.Private), None)
    val imageAssetDim = ImageAssetDimensions(2,2)
    val imageAsset = ImageAsset(None, "name", Some(imageAssetDim), None)
    val image = Image(List(imageAsset), Some(imageAsset), "ididididid")
    roundTrip(MediaAtom(assets, None, "Title", Category.Documentary, None, None, None, Some("url"), None, Some(metadata), Some(image)))
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
