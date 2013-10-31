package xitrum.util

import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.Serialization

object Json {
  private implicit val generateFormats = Serialization.formats(NoTypeHints)
  private implicit val parseFormats    = DefaultFormats

  /**
   * Generates JSON string from case objects etc.
   * See https://github.com/json4s/json4s#serialization
   */
  def generate(caseObject: AnyRef): String =
    Serialization.write(caseObject)(generateFormats)

  /**
   * Parses JSON string to case object.
   * See https://github.com/json4s/json4s#serialization
   */
  def parse[T](jsonString: String)(implicit m: Manifest[T]): T =
    Serialization.read[T](jsonString)(parseFormats, m)
}
