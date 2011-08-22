package xitrum.scope.session

import java.security.{MessageDigest, SecureRandom}
import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}

import xitrum.Config

/**
 * See https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/cookie.clj
 *
 * SecureBase64 is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user data.
 */
object SecureBase64 {
  def encrypt(value: Any): String = {
    val bytes = SeriDeseri.serialize(value)
    seal(key, bytes)
  }

  def decrypt(base64String: String): Option[Any] = {
    unseal(key, base64String) match {
      case None        => None
      case Some(bytes) => SeriDeseri.deserialize(bytes)
    }
  }

  //----------------------------------------------------------------------------

  // Algorithm to seed random numbers
  private val SEED_ALGORITHM = "SHA1PRNG"

  // Algorithm to generate a HMAC
  private val HMAC_ALGORITHM = "HmacSHA256"

  // Type of encryption to use
  private val CRYPT_TYPE = "AES"

  // Full algorithm to encrypt data with
  private val CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding"

  private val SEAL_SEPARATOR = "-xitrum-"

  /** AES compitable key computed from Config.secureKey */
  private val key: Array[Byte] = {
    // We need 16 bytes array for AES
    // MD5 => 16 bytes, SHA-256 => 32 bytes
    // Idea: http://stackoverflow.com/questions/992019/java-256bit-aes-encryption/992413
    val messageDigest = MessageDigest.getInstance("MD5")
    messageDigest.reset
    messageDigest.update(Config.secureKey.getBytes("UTF-8"))
    messageDigest.digest
  }

  /** @return a random byte array of the specified size. */
  private def secureRandomBytes(size: Int): Array[Byte] = {
    val seed = new Array[Byte](size)
    SecureRandom.getInstance(SEED_ALGORITHM).nextBytes(seed)
    seed
  }

  private def hmac(key: Array[Byte], data: Array[Byte]) = {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(new SecretKeySpec(key, HMAC_ALGORITHM))
    mac.doFinal(data)
  }

  private def encrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher    = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey = new SecretKeySpec(key, CRYPT_TYPE)
    val iv        = secureRandomBytes(cipher.getBlockSize)

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv))
    iv ++ cipher.doFinal(data)
  }

  private def decrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher      = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey   = new SecretKeySpec(key, CRYPT_TYPE)
    val (iv, data2) = data.splitAt(cipher.getBlockSize)
    val ivSpec      = new IvParameterSpec(iv)

    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    cipher.doFinal(data2)
  }

  private def seal(key: Array[Byte], data: Array[Byte]): String = {
    val data2 = encrypt(key, data)
    Base64.encode(data2) + SEAL_SEPARATOR + Base64.encode(hmac(key, data2))
  }

  private def unseal(key: Array[Byte], base64String: String): Option[Array[Byte]] = {
    try {
      val a = base64String.split(SEAL_SEPARATOR)
      val base64Data = a(0)
      val base64hmac = a(1)

      Base64.decode(base64Data) match {
        case None        => None
        case Some(data2) => if (base64hmac == Base64.encode(hmac(key, data2))) Some(decrypt(key, data2)) else None
      }
    } catch {
      case e => None
    }
  }
}
