package xitrum.util

import com.twitter.chill.KryoInjection
import xitrum.Config

/** Combination of Secure and UrlSafeBase64. */
object SecureUrlSafeBase64 {
  def encrypt(ref: AnyRef, forCookie: Boolean = false): String =
    encrypt(ref, Config.xitrum.session.secureKey, forCookie)

  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   *
   * @param forCookie If true, tries to GZIP compress if > 4KB; the result may > 4KB
   */
  def encrypt(ref: AnyRef, key: String, forCookie: Boolean): String = {
    val bytes           = KryoInjection(ref)
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

  def decrypt(base64String: String, forCookie: Boolean = false): Option[Any] =
    decrypt(base64String, Config.xitrum.session.secureKey, forCookie)

  /**
   * @param base64String may contain optional padding ("=" characters)
   * @param forCookie If true, tries to GZIP uncompress if the input is compressed
   */
  def decrypt(base64String: String, key: String, forCookie: Boolean): Option[Any] = {
    try {
      UrlSafeBase64.autoPaddingDecode(base64String).flatMap { encrypted =>
        Secure.decrypt(encrypted, key).flatMap { maybeCompressed =>
          val bytes = if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
          KryoInjection.invert(bytes)
        }
      }
    } catch {
      case scala.util.control.NonFatal(e) =>
        None
    }
  }
}
