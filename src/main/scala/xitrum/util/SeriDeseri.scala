package xitrum.util

import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import com.twitter.chill.KryoInjection
import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.{JsonMethods, Serialization}

import xitrum.Log

object SeriDeseri extends Log {
  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  def toBytes(any: Any): Array[Byte] = KryoInjection(any)

  def fromBytes[T](bytes: Array[Byte])(implicit m: Manifest[T]): Option[T] = {
    KryoInjection.invert(bytes) match {
      case Failure(e) =>
        None

      case Success(any) =>
        try {
          Some(any.asInstanceOf[T])
        } catch {
          case NonFatal(e) =>
            None
        }
    }
  }

  /**
   * Converts Scala object (case class, Map, Seq etc.) to JSON.
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def toJson(scalaObject: AnyRef): String =
    Serialization.write(scalaObject)(noTypeHints)

  /**
   * Converts JSON to Scala object (case class, Map, Seq etc.).
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def fromJson[T](jsonString: String)(implicit m: Manifest[T]): Option[T] = {
    // Serialization.read doesn't work without type hints.
    //
    // Serialization.read[Map[String, Any]]("""{"name": "X", "age": 45}""")
    // will throw:
    // org.json4s.package$MappingException: No information known about type
    //
    // JsonMethods.parse works for the above.
    if (m.runtimeClass.getName.startsWith("scala")) {
      try {
        Some(JsonMethods.parse(jsonString).values.asInstanceOf[T])
      } catch {
        case NonFatal(e) =>
          try {
            Some(Serialization.read[T](jsonString)(DefaultFormats, m))
          } catch {
            case NonFatal(e) =>
              None
          }
      }
    } else {
      try {
        Some(Serialization.read[T](jsonString)(DefaultFormats, m))
      } catch {
        case NonFatal(e) =>
          try {
            Some(JsonMethods.parse(jsonString).values.asInstanceOf[T])
          } catch {
            case NonFatal(e) =>
              None
          }
      }
    }
  }
}
