package xitrum.util

import scala.runtime.ScalaRunTime
import com.twitter.chill.KryoBijection
import xitrum.Logger

object SeriDeseri extends Logger {
  def serialize(ref: AnyRef): Array[Byte] = KryoBijection(ref)

  def deserialize(bytes: Array[Byte]): Option[AnyRef] = {
    // KryoInjection is not used because it swallows the debug info
    try {
      Some(KryoBijection.invert(bytes))
    } catch {
      case scala.util.control.NonFatal(e) =>
        if (logger.isDebugEnabled) logger.debug("Could not deserialize: " + ScalaRunTime.stringOf(bytes), e)
        None
    }
  }
}
