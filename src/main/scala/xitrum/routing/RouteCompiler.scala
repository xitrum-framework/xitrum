package xitrum.routing

object RouteCompiler {
  /**
   * Given the pattern "/articles/:id<[0-9]+>", compiles to
   * Seq(RouteToken("articles", true, None), RouteToken("id", false, Some("[0-9]+".r)))
   */
  def compile(pattern: String): Seq[RouteToken] = {
    // See PathInfo#tokens
    // Remove slash prefix if any so that app developers can write
    // "/articles" or "articles" and the resuls are the same
    val noSlashPrefix = if (pattern.startsWith("/")) pattern.substring(1) else pattern
    val fragments     = noSlashPrefix.split("/", -1)
    fragments.map(compilePatternFragment _)
  }

  def decompile(routeTokens: Seq[RouteToken], swagger: Boolean = false): String = {
    if (routeTokens.isEmpty) {
      "/"
    } else {
      routeTokens.foldLeft("") { (acc, t) =>
        if (swagger) {
          val rawValue =
            if (t.isPlaceHolder) {
              "{" + t.value + "}"
            } else {
              t.value
            }
          acc + "/" + rawValue
        } else {
          val rawValue =
            if (t.isPlaceHolder) {
              ":" + t.value
            } else {
              t.value
            }
          val rawRegex = t.regex match {
            case None => ""
            case Some(r) =>
              val string                = r.toString
              val withoutGraveAndDollar = string.substring(1, string.length - 1)
              "<" + withoutGraveAndDollar + ">"
          }
          acc + "/" + rawValue + rawRegex
        }
      }
    }
  }

  //----------------------------------------------------------------------------

  private def compilePatternFragment(fragment: String): RouteToken = {
    val isPlaceHolder         = fragment.startsWith(":")
    val regexMarkerStartIndex = fragment.indexOf("<")

    val value =
      if (regexMarkerStartIndex < 0) {
        if (isPlaceHolder) fragment.substring(1) else fragment
      } else {
        val valueStartIndex = if (isPlaceHolder) 1 else 0
        fragment.substring(valueStartIndex, regexMarkerStartIndex)
      }

    val regex =
      if (regexMarkerStartIndex < 0) {
        None
      } else {
        val regexString = fragment.substring(regexMarkerStartIndex + 1, fragment.length - 1)
        Some(("^" + regexString + "$").r)
      }
    RouteToken(value, isPlaceHolder, regex)
  }
}
