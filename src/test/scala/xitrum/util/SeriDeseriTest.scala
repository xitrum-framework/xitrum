package xitrum.util

import org.scalatest.{FlatSpec, Matchers}

class SeriDeseriTest extends FlatSpec with Matchers {
  behavior of "SeriDeseri"

  "bytesToBase64" should "serialize bytes to base64 with padding" in {
    SeriDeseri.bytesToBase64(Array[Byte](1, 2, 3, 4)) should equal ("AQIDBA==")
  }

  "bytesFromBase64" should "deserialize valid base64 to Some(bytes)" in {
    val o = SeriDeseri.bytesFromBase64("AQIDBA==")
    o should be ('defined)

    val a = o.get
    a should equal (Array[Byte](1, 2, 3, 4))
  }

  "bytesFromBase64" should "deserialize invalid base64 to None" in {
    SeriDeseri.bytesFromBase64("^^^") should equal (None)
  }

  "toSecureUrlSafeBase64 and fromSecureUrlSafeBase64" should "serialize and deserialize" in {
    val map1       = Map("csrf-token" -> "4ad194c6-06f4-41fb-86b8-8a4fc6053ab6")
    val serialized = SeriDeseri.toSecureUrlSafeBase64(map1, "key", true)
    val map2       = SeriDeseri.fromSecureUrlSafeBase64[Map[String, String]](serialized, "key", true)
    map2.get should equal (map1)
  }
}
