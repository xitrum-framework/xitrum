package xitrum.util

import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.Serialization

object Json {
  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  /**
   * Generates JSON string from Scala object (case class, Map, Seq etc.).
   * See https://github.com/json4s/json4s#serialization
   */
  def generate(scalaObject: AnyRef): String =
    Serialization.write(scalaObject)(noTypeHints)

  /**
   * Parses JSON string to Scala object (case class, Map, Seq etc.).
   * See https://github.com/json4s/json4s#serialization
   */
  def parse[T](jsonString: String)(implicit m: Manifest[T]): T =
    Serialization.read[T](jsonString)(DefaultFormats, m)
}
