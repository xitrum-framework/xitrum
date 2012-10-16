package xitrum.util

import xitrum.Config

object SecureBase64 {
  def encrypt(value: Any, compress: Boolean = false): String =
    encrypt(value, Config.config.session.secureKey, compress)

  def decrypt(base64String: String, compress: Boolean = false): Option[Any] =
    decrypt(base64String, Config.config.session.secureKey, compress)

  def encrypt(value: Any, key: String, compress: Boolean): String = {
    val bytes      = SeriDeseri.serialize(value)
    val compressed = if (compress) Gzip.compress(bytes) else bytes
    val encrypted  = Secure.encrypt(compressed, key)
    Base64.encode(encrypted)
  }

  def decrypt(base64String: String, key: String, compress: Boolean): Option[Any] = {
    try {
      Base64.decode(base64String).flatMap { encrypted =>
        Secure.decrypt(encrypted, key).flatMap { compressed =>
          val bytes = if (compress) Gzip.uncompress(compressed) else compressed
          SeriDeseri.deserialize(bytes)
        }
      }
    } catch {
      case _ =>
        None
    }
  }
}
