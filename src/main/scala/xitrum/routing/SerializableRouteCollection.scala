package xitrum.routing

import java.io.Serializable
import scala.collection.mutable.ArrayBuffer
import xitrum.Action

class SerializableRouteCollection extends Serializable {
  val firstGETs = ArrayBuffer[SerializableRoute]()
  val lastGETs  = ArrayBuffer[SerializableRoute]()
  val otherGETs = ArrayBuffer[SerializableRoute]()

  val firstPOSTs = ArrayBuffer[SerializableRoute]()
  val lastPOSTs  = ArrayBuffer[SerializableRoute]()
  val otherPOSTs = ArrayBuffer[SerializableRoute]()

  val firstPUTs = ArrayBuffer[SerializableRoute]()
  val lastPUTs  = ArrayBuffer[SerializableRoute]()
  val otherPUTs = ArrayBuffer[SerializableRoute]()

  val firstPATCHs = ArrayBuffer[SerializableRoute]()
  val lastPATCHs  = ArrayBuffer[SerializableRoute]()
  val otherPATCHs = ArrayBuffer[SerializableRoute]()

  val firstDELETEs = ArrayBuffer[SerializableRoute]()
  val lastDELETEs  = ArrayBuffer[SerializableRoute]()
  val otherDELETEs = ArrayBuffer[SerializableRoute]()

  val firstWEBSOCKETs = ArrayBuffer[SerializableRoute]()
  val lastWEBSOCKETs  = ArrayBuffer[SerializableRoute]()
  val otherWEBSOCKETs = ArrayBuffer[SerializableRoute]()

  // 404.html and 500.html are used by default
  var error404: Option[String] = None
  var error500: Option[String] = None
}
