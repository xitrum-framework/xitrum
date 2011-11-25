package xitrum.util

import xitrum.Config

object SecureBase64 {
  def encrypt(value: Any): String =
    encrypt(value, Config.config.session.secureKey)

  def decrypt(base64String: String): Option[Any] =
    decrypt(base64String, Config.config.session.secureKey)

  def encrypt(value: Any, key: String): String = {
    val bytes     = SeriDeseri.serialize(value)
    val encrypted = Secure.encrypt(bytes, key)
    Base64.encode(encrypted)
  }

  def decrypt(base64String: String, key: String): Option[Any] = {
    Base64.decode(base64String).flatMap { bytes =>
      Secure.decrypt(bytes, key).flatMap { bytes2 =>
        SeriDeseri.deserialize(bytes2)
      }
    }
  }
}
