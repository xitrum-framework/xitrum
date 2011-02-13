package xt.vc.env.session

import java.security.SecureRandom
import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}
import xt.Config

/** See https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/cookie.clj */
object SecureBase64 {
  def serialize(value: Any): String = {
    val bytes = SeriDeseri.serialize(value)
    seal(key, bytes)
  }

  def deserialize(secureBase64String: String): Option[Any] = {
    unseal(key, secureBase64String) match {
      case None        => None
      case Some(bytes) => SeriDeseri.deserialize(bytes)
    }
  }

  //----------------------------------------------------------------------------

  private val key = Config.secureBase64Key.getBytes("UTF-8")

  // Algorithm to seed random numbers
  private val SEED_ALGORITHM = "SHA1PRNG"

  // Algorithm to generate a HMAC
  private val HMAC_ALGORITHM = "HmacSHA256"

  // Type of encryption to use
  private val CRYPT_TYPE = "AES"

  // Full algorithm to encrypt data with
  private val CRYPT_ALGORITHM = "AES/CBC/PKCS5Padding"

  /** @return a random byte array of the specified size. */
  private def secureRandomBytes(size: Int): Array[Byte] = {
    val seed = new Array[Byte](size)
    SecureRandom.getInstance(SEED_ALGORITHM).nextBytes(seed)
    seed
  }

  /** Generates a Base64 HMAC with the supplied key on a string of data. */
  private def hmac(key: Array[Byte], data: Array[Byte]) = {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(new SecretKeySpec(key, HMAC_ALGORITHM))
    Base64.encode(mac.doFinal(data))
  }

  /** Encrypt a string with a key. */
  private def encrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher    = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey = new SecretKeySpec(key, CRYPT_TYPE)
    val iv        = secureRandomBytes(cipher.getBlockSize)

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv))
    iv ++ cipher.doFinal(data)
  }

  /** Decrypt an array of bytes with a key. */
  private def decrypt(key: Array[Byte], data: Array[Byte]): Array[Byte] = {
    val cipher      = Cipher.getInstance(CRYPT_ALGORITHM)
    val secretKey   = new SecretKeySpec(key, CRYPT_TYPE)
    val (iv, data2) = data.splitAt(cipher.getBlockSize)
    val ivSpec      = new IvParameterSpec(iv)

    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    cipher.doFinal(data2)
  }

  /** Seal a Clojure data structure into an encrypted and HMACed string. */
  private def seal(key: Array[Byte], data: Array[Byte]): String = {
    val data2 = encrypt(key, data)
    Base64.encode(data2) + "--" + hmac(key, data2)
  }

  /** Retrieve a sealed Clojure data structure from a string */
  private def unseal(key: Array[Byte], base64String: String): Option[Array[Byte]] = {
    try {
      val a = base64String.split("--")
      val data = a(0)
      val mac  = a(1)

      val data2 = Base64.decode(data)
      if (mac == hmac(key, data2)) Some(decrypt(key, data2)) else None
    } catch {
      case _ => None
    }
  }
}
