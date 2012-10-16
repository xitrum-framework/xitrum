package xitrum.util

//import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.jboss.serial.io.{JBossObjectInputStream, JBossObjectOutputStream}

/** JBoss Serialization is used: http://www.jboss.org/serialization */
object SeriDeseri {
  def serialize(value: Any): Array[Byte] = {
    val baos  = new ByteArrayOutputStream
    //val oos   = new ObjectOutputStream(baos)
    val oos   = new JBossObjectOutputStream(baos)
    oos.writeObject(value)
    val bytes = baos.toByteArray
    oos.close()
    baos.close()
    bytes
  }

  def deserialize(bytes: Array[Byte]): Option[Any] = {
    try {
      val bais  = new ByteArrayInputStream(bytes)
      //val ois   = new ObjectInputStream(bais)
      val ois   = new JBossObjectInputStream(bais)
      val value = ois.readObject
      ois.close()
      bais.close()
      Some(value)
    } catch {
      case _ => None
    }
  }
}
