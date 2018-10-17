package xitrum.util

import org.scalatest.{FlatSpec, Matchers}

class PathSanitizerTest extends FlatSpec with Matchers {
  // "Let's Encrypt" uses .well-known directory
  "sanitize" should "allow dot directory" in {
    PathSanitizer.sanitize("/.well-known") should equal (Some("/.well-known"))
  }

  "sanitize" should "disallow dot dot" in {
    PathSanitizer.sanitize("/../upper-directory") should equal (None)
  }

  "sanitize" should "allow normal file" in {
    PathSanitizer.sanitize("/a/b/c.png") should equal (Some("/a/b/c.png"))
  }

  "sanitize" should "replace backward slash with forward slash" in {
    PathSanitizer.sanitize("\\a\\b\\c.png") should equal (Some("/a/b/c.png"))
  }
}
