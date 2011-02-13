package xt.vc.env.session

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream, Serializable}
import scala.collection.mutable.{HashMap => MHashMap}
import sun.misc.{BASE64Decoder, BASE64Encoder}

class CookieSession extends Session {
  private var map = new MHashMap[String, Serializable]

  def deserialize(base64String: String) {
    val decoder = new BASE64Decoder
    val bytes   = decoder.decodeBuffer(base64String)
    val bais    = new ByteArrayInputStream(bytes)
    val ois     = new ObjectInputStream(bais)
    map         = ois.readObject.asInstanceOf[MHashMap[String, Serializable]]
    ois.close
    bais.close
  }

  def serialize: String = {
    val baos = new ByteArrayOutputStream
    val oos  = new ObjectOutputStream(baos)
    oos.writeObject(map)
    val bytes = baos.toByteArray
    oos.close
    baos.close

    val encoder = new BASE64Encoder
    new String(encoder.encodeBuffer(bytes))
  }

  def apply(key: String): Option[Any] = map.get(key)

  def update(key: String, value: Any) { map(key) = value.asInstanceOf[Serializable] }

  def delete(key: String) { map.remove(key) }

  def reset { map.clear }
}
