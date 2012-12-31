package xitrum.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.jboss.marshalling.{Marshalling, MarshallingConfiguration}
import org.jboss.marshalling.river.Protocol

/** https://docs.jboss.org/author/display/JBMAR/Marshalling+API+quick+start */
object SeriDeseri {
  private val marshallerFactory = Marshalling.getProvidedMarshallerFactory("river")
  private val configuration     = new MarshallingConfiguration

  configuration.setVersion(Protocol.MAX_VERSION)

  def serialize(value: Any): Array[Byte] = {
    val baos       = new ByteArrayOutputStream
    val marshaller = marshallerFactory.createMarshaller(configuration)
    marshaller.start(Marshalling.createByteOutput(baos))
    marshaller.writeObject(value)
    marshaller.finish()

    val bytes = baos.toByteArray
    baos.close()
    bytes
  }

  def deserialize(bytes: Array[Byte]): Option[Any] = {
    try {
      val bais         = new ByteArrayInputStream(bytes)
      val unmarshaller = marshallerFactory.createUnmarshaller(configuration)
      unmarshaller.start(Marshalling.createByteInput(bais))

      val value = unmarshaller.readObject()
      unmarshaller.finish()
      bais.close()
      Some(value)
    } catch {
      case scala.util.control.NonFatal(e) => None
    }
  }
}
