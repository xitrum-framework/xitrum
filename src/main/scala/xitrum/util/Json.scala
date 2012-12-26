package xitrum.util

import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.native.Serialization

object Json {
  // Generates JSON string from case object.
  def generate(caseObject: AnyRef): String = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.write(caseObject)
  }

  // Parses JSON string to case object.
  def parse[T](jsonString: String)(implicit m: Manifest[T]) = {
    implicit val formats = DefaultFormats
    Serialization.read[T](jsonString)
  }
}
