package xitrum.util

import scala.util.control.NonFatal
import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.{JsonMethods, Serialization}

/**
 * If you want to do more complicated things, you should use JSON4S directly:
 * https://github.com/json4s/json4s
 */
object Json {
  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  /**
   * Converts Scala object (case class, Map, Seq etc.) to JSON.
   * See https://github.com/json4s/json4s#serialization
   */
  def serialize(scalaObject: AnyRef): String =
    Serialization.write(scalaObject)(noTypeHints)

  /**
   * Converts JSON to Scala object (case class, Map, Seq etc.).
   * See https://github.com/json4s/json4s#serialization
   */
  def deserialize[T](jsonString: String)(implicit m: Manifest[T]): T = {
    // Serialization.read doesn't work without type hints.
    //
    // Serialization.read[Map[String, Any]]("""{"name": "X", "age": 45}""")
    // will throw:
    // org.json4s.package$MappingException: No information known about type
    //
    // JsonMethods.parse works for the above.
    if (m.runtimeClass.getName.startsWith("scala")) {
      try {
        JsonMethods.parse(jsonString).values.asInstanceOf[T]
      } catch {
        case NonFatal(e) =>
          Serialization.read[T](jsonString)(DefaultFormats, m)
      }
    } else {
      try {
        Serialization.read[T](jsonString)(DefaultFormats, m)
      } catch {
        case NonFatal(e) =>
          JsonMethods.parse(jsonString).values.asInstanceOf[T]
      }
    }
  }
}
