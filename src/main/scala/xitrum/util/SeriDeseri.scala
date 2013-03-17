package xitrum.util

import com.twitter.chill.KryoBijection

object SeriDeseri {
  def serialize(ref: AnyRef): Array[Byte] = KryoBijection(ref)

  def deserialize(bytes: Array[Byte]): Option[Any] = {
    try {
      Some(KryoBijection.invert(bytes))
    } catch {
      case scala.util.control.NonFatal(e) => None
    }
  }
}
