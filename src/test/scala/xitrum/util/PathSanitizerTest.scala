package xitrum.util

import java.io.File
import org.scalatest.{FlatSpec, Matchers}

class PathSanitizerTest extends FlatSpec with Matchers {
  // "Let's Encrypt" uses .well-known directory
  "sanitize" should "allow dot directory" in {
    PathSanitizer.sanitize("/.well-known") should equal (Some(File.separatorChar + ".well-known"))
  }

  "sanitize" should "disallow dot dot" in {
    PathSanitizer.sanitize("/../upper-directory") should equal (None)
  }

  "sanitize" should "allow normal file" in {
    PathSanitizer.sanitize("/a/b/c.png") should equal (Some(Seq("a", "b", "c.png").mkString(File.separator, File.separator, "")))
  }
}
