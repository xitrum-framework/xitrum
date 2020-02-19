package xitrum.routing

import java.io.Serializable
import scala.collection.mutable.ArrayBuffer

class SerializableRouteCollection extends Serializable {
  val firstGETs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastGETs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherGETs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  val firstPOSTs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastPOSTs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherPOSTs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  val firstPUTs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastPUTs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherPUTs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  val firstPATCHs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastPATCHs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherPATCHs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  val firstDELETEs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastDELETEs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherDELETEs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  val firstWEBSOCKETs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val lastWEBSOCKETs : ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]
  val otherWEBSOCKETs: ArrayBuffer[SerializableRoute] = ArrayBuffer.empty[SerializableRoute]

  // 404.html and 500.html are used by default
  var error404: Option[String] = None
  var error500: Option[String] = None

  override def toString: String =
    s"GET: ${firstGETs ++ otherGETs ++ lastGETs}\n" +
    s"POST: ${firstPOSTs ++ otherPOSTs ++ lastPOSTs}\n" +
    s"PUT: ${firstPUTs ++ otherPUTs ++ lastPUTs}\n" +
    s"PATCH: ${firstPATCHs ++ otherPATCHs ++ lastPATCHs}\n" +
    s"DELETE: ${firstDELETEs ++ otherDELETEs ++ lastDELETEs}\n" +
    s"WEBSOCKET: ${firstWEBSOCKETs ++ otherWEBSOCKETs ++ lastWEBSOCKETs}\n" +
    s"error404: $error404, error500: $error500"
}
