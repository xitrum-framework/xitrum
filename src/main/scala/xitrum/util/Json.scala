package xitrum.util

import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.Serialization

object Json {
  /**
   * Generates JSON string from case objects etc.
   * See https://github.com/json4s/json4s#serialization
   */
  def generate(caseObject: AnyRef): String = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.write(caseObject)
  }

  /**
   * Parses JSON string to case object.
   * See https://github.com/json4s/json4s#serialization
   */
  def parse[T](jsonString: String)(implicit m: Manifest[T]) = {
    implicit val formats = DefaultFormats
    Serialization.read[T](jsonString)
  }
}
