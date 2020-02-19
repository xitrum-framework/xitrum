package xitrum.util

object PathSanitizer {
  /** @return None if the path is suspicious (starts with ../ etc.) */
  def sanitize(path: String): Option[String] = {
    // Convert file separators
    val path2 = path.replace('\\', '/')

    // Simplistic dumb security check
    if (path2.startsWith("../") || path2.contains("/../")) {
      None
    } else {
      Some(path2)
    }
  }
}
