package xitrum.comet

import scala.collection.mutable.ListBuffer

import org.jboss.netty.handler.codec.http.HttpHeaders
import HttpHeaders.Names._
import HttpHeaders.Values._

import xitrum.Controller
import xitrum.scope.request.Params

object CometController extends CometController

class CometController extends Controller {
  def index = GET("xitrum/comet/:topic/:lastTimestamp") {
    val topic         = param("topic")
    val lastTimestamp = param[Long]("lastTimestamp")

    val listener = (messages: Seq[CometMessage]) => {
      val (timestamps, bodies) = messages.foldLeft((ListBuffer[Long](), ListBuffer[Params]())) { case ((ts, bs), m) =>
        ts.append(m.timestamp)
        bs.append(m.body)
        (ts, bs)
      }

      // Prevent browser side caching
      response.setHeader(CACHE_CONTROL, NO_CACHE)
      response.setHeader(PRAGMA, NO_CACHE)

      respondJson(Map("topic" -> topic, "timestamps" -> timestamps.toList, "bodies" -> bodies.toList))

      // Return true for Comet to automatically remove this listener.
      // With normal HTTP (nonwebsocket) the response can only be sent once.
      true
    }

    if (Comet.subscribe(topic, listener, lastTimestamp))
      // Avoid memory leak when messagePublished is never removed, e.g. no message is published
      addConnectionClosedListener { Comet.unsubscribe(topic, listener) }
  }

  def publish = POST("xiturm/comet/:topic") {
    val topic = param("topic")
    Comet.publish(topic, textParams - "topic")  // Save some memory
    respondText("")
  }
}
