package xitrum.util

import scala.util.control.NonFatal

import com.twitter.chill.{KryoInstantiator, KryoPool, KryoSerializer}
import org.json4s.{DefaultFormats, NoTypeHints}
import org.json4s.jackson.{JsonMethods, Serialization}

import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.{Base64, Base64Dialect}
import io.netty.util.CharsetUtil

import xitrum.Config

object SeriDeseri {
  // Use this utility instead of using Kryo directly because Kryo is not threadsafe!
  // https://github.com/EsotericSoftware/kryo#threading
  private val kryoPool = {
    val r  = KryoSerializer.registerAll
    val ki = (new KryoInstantiator).withRegistrar(r)
    KryoPool.withByteArrayOutputStream(Runtime.getRuntime.availableProcessors * 2, ki)
  }

  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  def toBytes(any: Any): Array[Byte] = kryoPool.toBytesWithoutClass(any)

  def fromBytes[T](bytes: Array[Byte])(implicit m: Manifest[T]): Option[T] = {
    try {
      val t = kryoPool.fromBytes(bytes, m.runtimeClass.asInstanceOf[Class[T]])
      Option(t)
    } catch {
      case NonFatal(e) => None
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
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   */
  def toUrlSafeBase64(bytes: Array[Byte]): String = {
    // No line break because the result may be used in HTTP response header (cookie)
    val buffer       = Base64.encode(Unpooled.wrappedBuffer(bytes), false, Base64Dialect.URL_SAFE)
    val base64String = buffer.toString(CharsetUtil.UTF_8)
    removeUrlSafeBase64Padding(base64String)
  }

  /** @param base64String may contain optional padding ("=" characters) */
  def fromUrlSafeBase64(base64String: String): Option[Array[Byte]] = {
    try {
      val withPadding = addUrlSafeBase64Padding(base64String)
      val buffer      = Base64.decode(Unpooled.copiedBuffer(withPadding, CharsetUtil.UTF_8), Base64Dialect.URL_SAFE)
      val bytes       = ByteBufUtil.toBytes(buffer)
      buffer.release()
      Some(bytes)
    } catch {
      case NonFatal(e) => None
    }
  }

  private def removeUrlSafeBase64Padding(base64String: String) = base64String.replace("=", "")

  private def addUrlSafeBase64Padding(base64String: String) = {
    val mod = base64String.length % 4
    val padding = if (mod == 0) "" else if (mod == 1) "===" else if (mod == 2) "==" else if (mod == 3) "="
    base64String + padding
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
    val ret1            = toUrlSafeBase64(encrypted)

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
        toUrlSafeBase64(encrypted)
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
    fromUrlSafeBase64(base64String).flatMap { encrypted =>
      Secure.decrypt(encrypted, key).flatMap { maybeCompressed =>
        val bytes = if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
        fromBytes[T](bytes)(m)
      }
    }
  }
}
