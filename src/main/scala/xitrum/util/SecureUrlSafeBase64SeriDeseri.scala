package xitrum.util

import xitrum.Config

/** Combination of Secure and UrlSafeBase64. */
object SecureUrlSafeBase64SeriDeseri {
  /** Encrypts using the key in config/xitrum.conf. */
  def toString(any: Any, forCookie: Boolean = false): String =
    toString(any, Config.xitrum.session.secureKey, forCookie)

  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   *
   * @param forCookie If true, tries to GZIP compress if > 4KB; the result may > 4KB
   */
  def toString(any: Any, key: String, forCookie: Boolean): String = {
    val bytes           = SeriDeseri.toBytes(any)
    val bytesCompressed = (forCookie && bytes.length > 4 * 1024)

    val maybeCompressed = if (bytesCompressed) Gzip.compress(bytes) else bytes
    val encrypted       = Secure.encrypt(maybeCompressed, key)
    val ret1            = UrlSafeBase64.noPaddingEncode(encrypted)

    if (!forCookie) {
      // Not cookie, nothing to do
      ret1
    } else {
      // bytes may <= 4KB but the final result may > 4KB!!!
      if (ret1.length <= 4 * 1024) {
        ret1
      } else if (bytesCompressed) {
        // bytes has been compressed, nothing more can be done to make the final result smaller
        ret1
      } else {
        // bytes has not been compressed, let's try
        val reallyCompressed = Gzip.compress(bytes)
        val encrypted        = Secure.encrypt(reallyCompressed, key)
        UrlSafeBase64.noPaddingEncode(encrypted)
      }
    }
  }

  /** Decrypts using the key in config/xitrum.conf. */
  def fromString[T](base64String: String, forCookie: Boolean = false)(implicit m: Manifest[T]): Option[T] =
    fromString[T](base64String, Config.xitrum.session.secureKey, forCookie)(m)

  /**
   * @param base64String may contain optional padding ("=" characters)
   * @param forCookie If true, tries to GZIP uncompress if the input is compressed
   */
  def fromString[T](base64String: String, key: String, forCookie: Boolean)(implicit m: Manifest[T]): Option[T] = {
    UrlSafeBase64.autoPaddingDecode(base64String).flatMap { encrypted =>
      Secure.decrypt(encrypted, key).flatMap { maybeCompressed =>
        val bytes = if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
        SeriDeseri.fromBytes(bytes)
      }
    }
  }
}
