package xitrum.util

import java.io.File

object PathSanitizer {
  /** @return None if the path is suspicious (starts with ../ etc.) */
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
