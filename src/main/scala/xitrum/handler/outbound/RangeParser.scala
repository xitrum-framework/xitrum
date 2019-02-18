package xitrum.handler.outbound

import xitrum.Log

import scala.util.control.NonFatal

sealed trait RangeParserResult

// Valid range is send by client
// Xitrum should return 206 (Partial content).
case class SatisfiableRange(startIndex: Long, endIndex: Long) extends RangeParserResult

// first-byte-pos value greater than the current length of the selected resource.
// Xitrum should return 416 (Requested Range Not Satisfiable).
case object UnsatisfiableRange extends RangeParserResult

// Unsupported format, include syntax error.
// Xitrum should ignore range header.
case object UnsupportedRange extends RangeParserResult

object RangeParser {
  /**
   * "Range" request: http://tools.ietf.org/html/rfc2616#section-14.35
   *
   * For simplicity only these specs are supported:
   * bytes=123-456
   * bytes=123-
   *
   * If the last-byte-pos value is present, it MUST be greater than or
   * equal to the first-byte-pos in that byte-range-spec, or the byte-
   * range-spec is syntactically invalid. The recipient of a byte-range-
   * set that includes one or more syntactically invalid byte-range-spec
   * values MUST ignore the header field that includes that byte-range-
   * set.
   *
   * If the last-byte-pos value is absent, or if the value is greater than
   * or equal to the current length of the entity-body, last-byte-pos is
   * taken to be equal to one less than the current length of the entity-
   * body in bytes.
   */
  def parse(spec: String, length: Long): RangeParserResult = {
    // Log unsupported Range specs, so that we know that we may need to support them later

    if (spec == null) {
      return UnsupportedRange
    }

    if (spec.length <= 6) {  // 6: length of "bytes="
      Log.warn("Unsupported Range spec: " + spec)
      return UnsupportedRange
    }

    // Skip "bytes="
    val range = spec.substring(6)

    // Split start and end
    val se = range.split('-')
    if (se.length != 1 && se.length != 2) {
      Log.warn("Unsupported Range spec: " + spec)
      return UnsupportedRange
    }

    // Catch toLong exception
    try {
      val s = se(0).toLong
      val e = if (se.length == 2) se(1).toLong else length - 1

      if (s > length - 1 || s > e) {
        UnsatisfiableRange
      } else {
        SatisfiableRange(s, Math.min(e, length - 1))
      }
    } catch {
      case NonFatal(_) =>
        Log.warn("Unsupported Range spec: " + spec)
        UnsupportedRange
    }
  }
}
