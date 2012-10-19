package xitrum.util

import xitrum.Config

object SecureBase64 {
  /** @param forCookie If true, tries to GZIP compress if > 4KB */
  def encrypt(value: Any, forCookie: Boolean = false): String =
    encrypt(value, Config.config.session.secureKey, forCookie)

  def encrypt(value: Any, key: String, forCookie: Boolean): String = {
    val bytes           = SeriDeseri.serialize(value)
    val maybeCompressed = if (bytes.length > 4 * 1024 && forCookie) Gzip.compress(bytes) else bytes
    val encrypted       = Secure.encrypt(maybeCompressed, key)
    Base64.encode(encrypted)
  }

  /** @param forCookie If true, tries to GZIP uncompress if the input is compressed */
  def decrypt(base64String: String, forCookie: Boolean = false): Option[Any] =
    decrypt(base64String, Config.config.session.secureKey, forCookie)

  def decrypt(base64String: String, key: String, forCookie: Boolean): Option[Any] = {
    try {
      Base64.decode(base64String).flatMap { encrypted =>
        Secure.decrypt(encrypted, key).flatMap { maybeCompressed =>
          val bytes = if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
          SeriDeseri.deserialize(bytes)
        }
      }
    } catch {
      case _ =>
        None
    }
  }
}
