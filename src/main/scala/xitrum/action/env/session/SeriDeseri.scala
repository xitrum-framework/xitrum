package xitrum.action.env.session

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream, Serializable}

object SeriDeseri {
  def serialize(value: Any): Array[Byte] = {
    val baos  = new ByteArrayOutputStream
    val oos   = new ObjectOutputStream(baos)
    oos.writeObject(value)
    val bytes = baos.toByteArray
    oos.close
    baos.close
    bytes
  }

  def deserialize(bytes: Array[Byte]): Option[Any] = {
    try {
      val bais  = new ByteArrayInputStream(bytes)
      val ois   = new ObjectInputStream(bais)
      val value = ois.readObject
      ois.close
      bais.close
      Some(value)
    } catch {
      case _ => None
    }
  }
}
