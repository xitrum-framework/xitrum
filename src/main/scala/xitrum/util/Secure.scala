package xitrum.util

import java.nio.ByteBuffer
import java.security.{MessageDigest, SecureRandom}
import java.util.Arrays

import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}

import org.jboss.netty.util.CharsetUtil.UTF_8
import xitrum.Config

/**
 * See https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/cookie.clj
 *
 * Note that Secure is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user's data.
 */
object Secure {
  /** Always returns same output for same input. */
  def encrypt(data: Array[Byte]): Array[Byte] =
    encrypt(data, Config.xitrum.session.secureKey)

  def decrypt(data: Array[Byte]): Option[Array[Byte]] =
    decrypt(data, Config.xitrum.session.secureKey)

  def encrypt(data: Array[Byte], key: String): Array[Byte] = {
    val bkey  = makeKey(key)
    val data2 = encryptWithoutSeal(data, bkey)
    seal(data2, bkey)
  }

  def decrypt(data: Array[Byte], key: String): Option[Array[Byte]] = {
    val bkey = makeKey(key)
    unseal(data, bkey).flatMap { data2 =>
      try {
        Some(decryptWithoutSeal(data2, bkey))
      } catch {
        case scala.util.control.NonFatal(e) => None
      }
    }
  }

  //----------------------------------------------------------------------------

  // Algorithm to generate a HMAC
  private[this] val HMAC_ALGORITHM = "HmacSHA256"

  // Type of encryption to use
  private[this] val CRYPT_TYPE = "AES"

  // Full algorithm to encrypt data with
  private[this] val CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding"

  // Cache for speed because this key is used most of the time
  private[this] val defaultKey = makeKey(Config.xitrum.session.secureKey)

  // We need 16 bytes array for AES
  // MD5 => 16 bytes, SHA-256 => 32 bytes
  // Idea: http://stackoverflow.com/questions/992019/java-256bit-aes-encryption/992413
  private def makeKey(key: String): Array[Byte] = {
    if (key == Config.xitrum.session.secureKey && defaultKey != null) {
      defaultKey
    } else {
      val messageDigest = MessageDigest.getInstance("MD5")
      messageDigest.reset
      messageDigest.update(key.getBytes(UTF_8))
      messageDigest.digest
    }
  }

  /** @return a byte array of the specified size. */
  private def makeBytes(size: Int): Array[Byte] = {
    // Do not return random bytes so that encrypt method
    // always returns same output for same input
    //
    // TODO: each user session should have different bytes?
    new Array[Byte](size)
  }

  private def hmac(data: Array[Byte], key: Array[Byte]) = {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(new SecretKeySpec(key, HMAC_ALGORITHM))
    mac.doFinal(data)
  }

  private def encryptWithoutSeal(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val cipher    = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey = new SecretKeySpec(key, CRYPT_TYPE)
    val iv        = makeBytes(cipher.getBlockSize)

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv))
    cipher.doFinal(data)
  }

  private def decryptWithoutSeal(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val cipher    = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey = new SecretKeySpec(key, CRYPT_TYPE)
    val iv        = makeBytes(cipher.getBlockSize)
    val ivSpec    = new IvParameterSpec(iv)

    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    cipher.doFinal(data)
  }

  // Anatomy of the result: <length of data><data><hmac of data>
  private def seal(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val l = data.length
    val h = hmac(data, key)
    val b = ByteBuffer.allocate(4 + l + h.length)
    b.putInt(l)
    b.put(data)
    b.put(h)
    b.array
  }

  // The reverse of seal method above
  private def unseal(data: Array[Byte], key: Array[Byte]): Option[Array[Byte]] = {
    try {
      if (data.length <= 4) return None

      val b = ByteBuffer.wrap(data)
      val l = b.getInt()
      if (l <= 0 || l >= data.length - 4) return None

      val d = new Array[Byte](l)
      b.get(d)
      val h = new Array[Byte](data.length - 4 - l)
      b.get(h)

      val h2 = hmac(d, key)
      if (Arrays.equals(h2, h)) Some(d) else None
    } catch {
      case scala.util.control.NonFatal(e) => None
    }
  }
}
