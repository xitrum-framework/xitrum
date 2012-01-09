package xitrum.comet

import scala.collection.mutable.ListBuffer

import io.netty.handler.codec.http.HttpHeaders
import HttpHeaders.Names._
import HttpHeaders.Values._

import xitrum.Controller
import xitrum.scope.request.Params

object CometController extends CometController

class CometController extends Controller {
  def index = GET("xitrum/comet/:channel/:lastTimestamp") {
    val channel       = param("channel")
    val lastTimestamp = param[Long]("lastTimestamp")

    val messages = Comet.getMessages(channel, lastTimestamp)

    // When there is no message, the connection is kept and the response will
    // be sent as soon as there a message arrives

    if (messages.isEmpty) {
      val messagePublished = (message: CometMessage) => {
        respondMessages(channel, List(message))

        // Return true for Comet to automatically remove this listener.
        // With HTTP the reponse can only be sent once.
        true
      }

      Comet.addMessageListener(channel, messagePublished)

      // Avoid memory leak when messagePublished is never removed, e.g. no message is published
      addConnectionClosedListener(() => Comet.removeMessageListener(channel, messagePublished))
    } else {
      // lastTimestamp = 0 is a fixed GET URL
      // We should prevent browser side caching
      if (lastTimestamp == 0) {
        response.setHeader(CACHE_CONTROL, "no-cache")
        response.setHeader(PRAGMA, "no-cache")
      }

      respondMessages(channel, messages)
    }
  }

  def publish = POST("xiturm/comet/:channel") {
    val channel = param("channel")
    Comet.publish(channel, textParams - "channel")  // Save some space
    respondText("")
  }

  //----------------------------------------------------------------------------

  private def respondMessages(channel: String, messages: Iterable[CometMessage]) {
    val (timestamps, bodies) = messages.foldLeft((ListBuffer[Long](), ListBuffer[Params]())) { case ((ts, bs), m) =>
      ts.append(m.timestamp)
      bs.append(m.body)
      (ts, bs)
    }
    respondJson(Map("channel" -> channel, "timestamps" -> timestamps.toList, "bodies" -> bodies.toList))
  }
}
