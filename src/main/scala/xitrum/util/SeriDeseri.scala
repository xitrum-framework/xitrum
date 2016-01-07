package xitrum.util

import scala.util.Try
import scala.util.control.NonFatal

import com.twitter.chill.{KryoInstantiator, KryoPool, KryoSerializer}
import org.json4s.{DefaultFormats, Extraction, JValue, NoTypeHints}
import org.json4s.jackson.{JsonMethods, Serialization}

import io.netty.buffer.{ByteBuf, Unpooled}
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

  def toBytes(any: Any): Array[Byte] = kryoPool.toBytesWithoutClass(any)

  def fromBytes[T](bytes: Array[Byte])(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    try {
      val t = kryoPool.fromBytes(bytes, m.runtimeClass.asInstanceOf[Class[T]])
      Option(t)
    } catch {
      case NonFatal(e) => None
    }
  }

  //----------------------------------------------------------------------------

  private implicit val noTypeHints = Serialization.formats(NoTypeHints)

  /**
   * Converts Scala object (case class, Map, Seq etc.) to JSON string.
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def toJson(scalaObject: AnyRef): String =
    Serialization.write(scalaObject)(noTypeHints)

  /**
   * Converts JSON string to Scala object (case class, Map, Seq etc.).
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def fromJson[T](jsonString: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] =
    JsonMethods.parseOpt(jsonString).flatMap { json => fromJValue[T](json)(e, m) }

  //----------------------------------------------------------------------------

  /**
   * Similar to [[toJson]], but the result is JSON4S [[JValue]] instead of string,
   * so that you can further transform it before rendering to string.
   */
  def toJValue(scalaObject: AnyRef): JValue =
    Extraction.decompose(scalaObject)

  /**
   * Converts JSON4S [[JValue]] to Scala object (case class, Map, Seq etc.).
   * If you want to do more complicated things, you should use JSON4S directly:
   * https://github.com/json4s/json4s
   */
  def fromJValue[T](jvalue: JValue)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    // Serialization.read doesn't work without type hints.
    //
    // Serialization.read[Map[String, Any]]("""{"name": "X", "age": 45}""")
    // will throw:
    // org.json4s.package$MappingException: No information known about type
    //
    // JsonMethods.parse works for the above.
    if (m.runtimeClass.getName.startsWith("scala")) {
      try {
        val any = jvalue.values
        if (m.runtimeClass.isAssignableFrom(any.getClass))
          Some(any.asInstanceOf[T])
        else
          None
      } catch {
        case NonFatal(e) =>
          try {
            Some(jvalue.extract[T](DefaultFormats, m))
          } catch {
            case NonFatal(e) =>
              None
          }
      }
    } else {
      try {
        Some(jvalue.extract[T](DefaultFormats, m))
      } catch {
        case NonFatal(e) =>
          try {
            val any = jvalue.values
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

  def bytesToBase64(bytes: Array[Byte]): String = {
    val src    = Unpooled.wrappedBuffer(bytes)
    val dest   = Base64.encode(src)
    val base64 = dest.toString(CharsetUtil.UTF_8)
    src.release()
    dest.release()
    base64
  }

  def bytesFromBase64(base64String: String): Option[Array[Byte]] = {
    var src:  ByteBuf = null
    var dest: ByteBuf = null

    try {
      src       = Unpooled.copiedBuffer(base64String, CharsetUtil.UTF_8)
      dest      = Base64.decode(src)
      val bytes = new Array[Byte](dest.readableBytes)
      dest.readBytes(bytes)
      Some(bytes)
    } catch {
      case NonFatal(e) =>
        None
    } finally {
      if (src  != null) src.release()
      if (dest != null) dest.release()
    }
  }

  //----------------------------------------------------------------------------

  /** @return Lower case hexadecimal string */
  def bytesToHex(bytes: Array[Byte]): String = {
    val sb = new StringBuilder(bytes.length * 2)
    for (b <- bytes) {
      val nonnegative = b & 0xff  // The byte may be negative
      sb.append("%02x".format(nonnegative))
    }
    sb.toString
  }

  def bytesFromHex(hex: String): Option[Array[Byte]] = {
    Try(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)).toOption
  }

  //----------------------------------------------------------------------------

  def toBase64(any: Any): String = {
    val bytes = toBytes(any)
    bytesToBase64(bytes)
  }

  def fromBase64[T](base64String: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    for {
      bytes <- bytesFromBase64(base64String)
      t     <- fromBytes(bytes)(e, m)
    } yield t
  }

  //----------------------------------------------------------------------------

  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   */
  def bytesToUrlSafeBase64(bytes: Array[Byte]): String = {
    // No line break because the result may be used in HTTP response header (cookie)
    val inBuffer     = Unpooled.wrappedBuffer(bytes)
    val outBuffer    = Base64.encode(inBuffer, false, Base64Dialect.URL_SAFE)
    val base64String = outBuffer.toString(CharsetUtil.UTF_8)
    outBuffer.release()
    inBuffer.release()
    removeUrlSafeBase64Padding(base64String)
  }

  /** @param base64String may contain optional padding ("=" characters) */
  def bytesFromUrlSafeBase64(base64String: String): Option[Array[Byte]] = {
    val withPadding = addUrlSafeBase64Padding(base64String)
    val inBuffer    = Unpooled.copiedBuffer(withPadding, CharsetUtil.UTF_8)
    try {
      val outBuffer = Base64.decode(inBuffer, Base64Dialect.URL_SAFE)
      val bytes     = ByteBufUtil.toBytes(outBuffer)
      outBuffer.release()
      Some(bytes)
    } catch {
      case NonFatal(e) => None
    } finally {
      inBuffer.release()
    }
  }

  private def removeUrlSafeBase64Padding(base64String: String) = base64String.replace("=", "")

  private def addUrlSafeBase64Padding(base64String: String) = {
    val mod     = base64String.length % 4
    val padding = mod match {
      case 1 => "==="
      case 2 => "=="
      case 3 => "="
      case _ => ""
    }
    base64String + padding
  }

  //----------------------------------------------------------------------------

  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   */
  def toUrlSafeBase64(any: Any): String = {
    val bytes = toBytes(any)
    bytesToUrlSafeBase64(bytes)
  }


  /** @param base64String may contain optional padding ("=" characters) */
  def fromUrlSafeBase64[T](base64String: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    for {
      bytes <- bytesFromUrlSafeBase64(base64String)
      t     <- fromBytes(bytes)(e, m)
    } yield t
  }

  //----------------------------------------------------------------------------

  /** Encrypts using the key in config/xitrum.conf. */
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
    val bytesCompressed = forCookie && bytes.length > 4 * 1024

    val maybeCompressed = if (bytesCompressed) Gzip.compress(bytes) else bytes
    val encrypted       = Secure.encrypt(maybeCompressed, key)
    val ret1            = bytesToUrlSafeBase64(encrypted)

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
        bytesToUrlSafeBase64(encrypted)
      }
    }
  }

  /** Decrypts using the key in config/xitrum.conf. */
  def fromSecureUrlSafeBase64[T](
    base64String: String, forCookie: Boolean = false
  )(
    implicit e: T DefaultsTo String, m: Manifest[T]
  ): Option[T] =
    fromSecureUrlSafeBase64[T](base64String, Config.xitrum.session.secureKey, forCookie)(e, m)

  /**
   * @param base64String may contain optional padding ("=" characters)
   * @param forCookie If true, tries to GZIP uncompress if the input is compressed
   */
  def fromSecureUrlSafeBase64[T](
    base64String: String, key: String, forCookie: Boolean
  )(
    implicit e: T DefaultsTo String, m: Manifest[T]
  ): Option[T] = {
    for {
      encrypted       <- bytesFromUrlSafeBase64(base64String)
      maybeCompressed <- Secure.decrypt(encrypted, key)
      bytes           =  if (forCookie) Gzip.mayUncompress(maybeCompressed) else maybeCompressed
      t               <- fromBytes[T](bytes)(e, m)
    } yield t
  }
}
