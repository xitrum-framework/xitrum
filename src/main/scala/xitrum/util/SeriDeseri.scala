package xitrum.util

import scala.runtime.ScalaRunTime
import scala.util.{Try, Success, Failure}

import com.twitter.chill.KryoInjection
import xitrum.Log

object SeriDeseri extends Log {
  def serialize(any: Any): Array[Byte] = KryoInjection(any)

  def deserialize(bytes: Array[Byte]): Option[Any] = {
    KryoInjection.invert(bytes) match {
      case Success(any) =>
        Some(any)

      case Failure(e) =>
        if (log.isDebugEnabled) log.debug("Could not deserialize: " + ScalaRunTime.stringOf(bytes), e)
        None
    }
  }
}
