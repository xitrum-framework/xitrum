package xitrum.util

import org.scalatest.Matchers
import org.scalatest.FlatSpec

class SeriDeseriTest extends FlatSpec with Matchers {
  behavior of "SeriDeseri"

  "toBase64" should "serialize bytes to base64 with padding" in {
    SeriDeseri.toBase64(Array[Byte](1, 2, 3, 4)) should equal ("AQIDBA==")
  }

  "fromBase64" should "deserialize valid base64 to Some(bytes)" in {
    val o = SeriDeseri.fromBase64("AQIDBA==")
    o should be ('defined)

    val a = o.get
    a should equal (Array[Byte](1, 2, 3, 4))
  }

  "fromBase64" should "deserialize invalid base64 to None" in {
    SeriDeseri.fromBase64("^^^") should equal (None)
  }
}
