package xitrum.routing

import java.io.Serializable
import scala.collection.mutable.ArrayBuffer

class SerializableRouteCollection extends Serializable {
  val firstGETs = ArrayBuffer.empty[SerializableRoute]
  val lastGETs  = ArrayBuffer.empty[SerializableRoute]
  val otherGETs = ArrayBuffer.empty[SerializableRoute]

  val firstPOSTs = ArrayBuffer.empty[SerializableRoute]
  val lastPOSTs  = ArrayBuffer.empty[SerializableRoute]
  val otherPOSTs = ArrayBuffer.empty[SerializableRoute]

  val firstPUTs = ArrayBuffer.empty[SerializableRoute]
  val lastPUTs  = ArrayBuffer.empty[SerializableRoute]
  val otherPUTs = ArrayBuffer.empty[SerializableRoute]

  val firstPATCHs = ArrayBuffer.empty[SerializableRoute]
  val lastPATCHs  = ArrayBuffer.empty[SerializableRoute]
  val otherPATCHs = ArrayBuffer.empty[SerializableRoute]

  val firstDELETEs = ArrayBuffer.empty[SerializableRoute]
  val lastDELETEs  = ArrayBuffer.empty[SerializableRoute]
  val otherDELETEs = ArrayBuffer.empty[SerializableRoute]

  val firstWEBSOCKETs = ArrayBuffer.empty[SerializableRoute]
  val lastWEBSOCKETs  = ArrayBuffer.empty[SerializableRoute]
  val otherWEBSOCKETs = ArrayBuffer.empty[SerializableRoute]

  // 404.html and 500.html are used by default
  var error404: Option[String] = None
  var error500: Option[String] = None

  override def toString =
    s"GET: ${firstGETs ++ otherGETs ++ lastGETs}\n" +
    s"POST: ${firstPOSTs ++ otherPOSTs ++ lastPOSTs}\n" +
    s"PUT: ${firstPUTs ++ otherPUTs ++ lastPUTs}\n" +
    s"PATCH: ${firstPATCHs ++ otherPATCHs ++ lastPATCHs}\n" +
    s"DELETE: ${firstDELETEs ++ otherDELETEs ++ lastDELETEs}\n" +
    s"WEBSOCKET: ${firstWEBSOCKETs ++ otherWEBSOCKETs ++ lastWEBSOCKETs}\n" +
    s"error404: $error404, error500: $error500"
}
