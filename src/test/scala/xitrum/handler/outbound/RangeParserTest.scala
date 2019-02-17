package xitrum.handler.outbound

import org.scalatest.{FlatSpec, Matchers}

class RangeParserTest extends FlatSpec with Matchers {
  behavior of "RangeParser"

  def test(spec: String, expected: RangeParserResult) {
    "parse" should s"handle $spec" in {
      RangeParser.parse(spec, 1048576) shouldBe expected
    }
  }

  // Invalid
  test(null, UnsupportedRange)
  test("bytes=", UnsupportedRange)
  test("bytes=-", UnsupportedRange)
  test("bytes=--", UnsupportedRange)
  test("bytes=0--1", UnsupportedRange)
  test("bytes=10-5", UnsatisfiableRange)
  test("bytes=1048576", UnsatisfiableRange)

  // last-byte-pos value is absent
  test("bytes=0", SatisfiableRange(0, 1048575))
  test("bytes=0-", SatisfiableRange(0, 1048575))
  test("bytes=1048574", SatisfiableRange(1048574, 1048575))

  // last-byte-pos value is present
  test("bytes=0-0", SatisfiableRange(0, 0))
  test("bytes=0-1", SatisfiableRange(0, 1))
  test("bytes=0-1048574", SatisfiableRange(0, 1048574))
  test("bytes=0-1048575", SatisfiableRange(0, 1048575))
  test("bytes=0-1048576", SatisfiableRange(0, 1048575))

  // first-byte-pos value greater than the length
  test("bytes=0-1048577", SatisfiableRange(0, 1048575))
}
