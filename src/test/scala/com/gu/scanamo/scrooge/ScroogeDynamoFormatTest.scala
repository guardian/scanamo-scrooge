package com.gu.scanamo.scrooge


import cats.data.Xor
import com.gu.contentatom.thrift.atom.media.AssetType
import org.scalatest.{FunSuite, Matchers}


class ScroogeDynamoFormatTest extends FunSuite with Matchers {

  test("testScroogeScanamoEnumFormat") {
    val format = ScroogeDynamoFormat.scroogeScanamoEnumFormat[AssetType]
    format.read(format.write(AssetType.Audio)) should be(Xor.right(AssetType.Audio))
    format.read(format.write(AssetType.Video)) should be(Xor.right(AssetType.Video))
  }

}
