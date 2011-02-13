package xt.vc.env.session

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream, Serializable}
import java.nio.charset.Charset
import scala.collection.mutable.{HashMap => MHashMap}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.{Base64, Base64Dialect}

class CookieSession extends Session {
  private var map = new MHashMap[String, Serializable]

  def deserialize(base64String: String) {
    try {
      val buffer = Base64.decode(ChannelBuffers.copiedBuffer(base64String, Charset.forName("UTF-8")), Base64Dialect.URL_SAFE)
      val bytes  = new Array[Byte](buffer.readableBytes)
      buffer.readBytes(bytes)

      val bais = new ByteArrayInputStream(bytes)
      val ois  = new ObjectInputStream(bais)
      map      = ois.readObject.asInstanceOf[MHashMap[String, Serializable]]
      ois.close
      bais.close
    } catch {
      case _ => map = new MHashMap[String, Serializable]
    }
  }

  def serialize: String = {
    val baos  = new ByteArrayOutputStream
    val oos   = new ObjectOutputStream(baos)
    oos.writeObject(map)
    val bytes = baos.toByteArray
    oos.close
    baos.close

    val buffer = Base64.encode(ChannelBuffers.copiedBuffer(bytes), Base64Dialect.URL_SAFE)
    buffer.toString(Charset.forName("UTF-8"))
  }

  def apply(key: String): Option[Any] = map.get(key)

  def update(key: String, value: Any) { map(key) = value.asInstanceOf[Serializable] }

  def delete(key: String) { map.remove(key) }

  def reset { map.clear }
}
