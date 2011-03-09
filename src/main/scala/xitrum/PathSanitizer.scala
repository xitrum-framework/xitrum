package xitrum

import java.io.File

object PathSanitizer {
  def sanitize(path: String): Option[String] = {
    // Convert file separators
    val path2 = path.replace('\\', File.separatorChar).replace('/', File.separatorChar)

    // Simplistic dumb security check
    if (path2.contains(File.separator + ".") ||
        path2.contains("." + File.separator) ||
        path2.startsWith(".")                ||
        path2.endsWith(".")) {
      None
    } else {
      Some(path2)
    }
  }
}
