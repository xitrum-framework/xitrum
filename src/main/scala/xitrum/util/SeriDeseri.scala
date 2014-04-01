package xitrum.util

import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import com.twitter.chill.KryoInjection
import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.jackson.{JsonMethods, Serialization}

import xitrum.Config

object SeriDeseri {
  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  def toBytes(any: Any): Array[Byte] = KryoInjection(any)

  def fromBytes[T](bytes: Array[Byte])(implicit m: Manifest[T]): Option[T] = {
    KryoInjection.invert(bytes) match {
      case Failure(e) =>
        None

      case Success(any) =>
        if (m.runtimeClass.isAssignableFrom(any.getClass))
          Some(any.asInstanceOf[T])
        else
          None
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Converts Scala object (case class, Map, Seq etc.) to JSON.
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def toJson(scalaObject: AnyRef): String =
    Serialization.write(scalaObject)(noTypeHints)

  /**
   * Converts JSON to Scala object (case class, Map, Seq etc.).
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def fromJson[T](jsonString: String)(implicit m: Manifest[T]): Option[T] = {
    // Serialization.read doesn't work without type hints.
    //
    // Serialization.read[Map[String, Any]]("""{"name": "X", "age": 45}""")
    // will throw:
    // org.json4s.package$MappingException: No information known about type
    //
    // JsonMethods.parse works for the above.
    if (m.runtimeClass.getName.startsWith("scala")) {
      try {
        val any = JsonMethods.parse(jsonString).values
        if (m.runtimeClass.isAssignableFrom(any.getClass))
          Some(any.asInstanceOf[T])
        else
          None
      } catch {
        case NonFatal(e) =>
          try {
            Some(Serialization.read[T](jsonString)(DefaultFormats, m))
          } catch {
            case NonFatal(e) =>
              None
          }
      }
    } else {
      try {
        Some(Serialization.read[T](jsonString)(DefaultFormats, m))
      } catch {
        case NonFatal(e) =>
          try {
            val any = JsonMethods.parse(jsonString).values
            if (m.runtimeClass.isAssignableFrom(any.getClass))
              Some(any.asInstanceOf[T])
            else
              None
          } catch {
            case NonFatal(e) =>
              None
          }
      }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Encrypts using the key in config/xitrum.conf.
   * Combination of Secure and UrlSafeBase64.
   */
  def toSecureUrlSafeBase64(any: Any, forCookie: Boolean = false): String =
    toSecureUrlSafeBase64(any, Config.xitrum.session.secureKey, forCookie)

  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   *
   * @param forCookie If true, tries to GZIP compress if > 4KB; the result may > 4KB
   */
  def toSecureUrlSafeBase64(any: Any, key: String, forCookie: Boolean): String = {
    val bytes           = toBytes(any)
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

  /**
   * Decrypts using the key in config/xitrum.conf.
   * Combination of Secure and UrlSafeBase64.
   */
  def fromSecureUrlSafeBase64[T](base64String: String, forCookie: Boolean = false)(implicit m: Manifest[T]): Option[T] =
    fromSecureUrlSafeBase64[T](base64String, Config.xitrum.session.secureKey, forCookie)(m)

  /**
   * @param base64String may contain optional padding ("=" characters)
   * @param forCookie If true, tries to GZIP uncompress if the input is compressed
   */
  def fromSecureUrlSafeBase64[T](base64String: String, key: String, forCookie: Boolean)(implicit m: Manifest[T]): Option[T] = {
    UrlSafeBase64.autoPaddingDecode(base64String).flatMap { encrypted =>
      Secure.decrypt(encrypted, key).flatMap { maybeCompressed =>
        val bytes = if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
        fromBytes[T](bytes)(m)
      }
    }
  }
}
