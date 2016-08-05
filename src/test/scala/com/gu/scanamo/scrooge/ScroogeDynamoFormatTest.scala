package com.gu.scanamo.scrooge

import cats.data.Xor
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.AtomData._
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media._
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

  test("testScroogeScanamoUnionFormat") {
    import ScroogeDynamoFormat._

    val atom = Atom("id", AtomType.Media, List("label"), "Html",
      AtomData.Media(MediaAtom(List(Asset(AssetType.Audio, 1L, "asset-id", Platform.Youtube)), 1L, None)),
      ContentChangeDetails(None, None, None, 1L), None)
    val av = DynamoFormat[Atom].write(atom)
    println(av)
    DynamoFormat[Atom].read(av) should be(Xor.right(atom))
  }
}