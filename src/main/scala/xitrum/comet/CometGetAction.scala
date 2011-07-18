package xitrum.comet

import scala.collection.mutable.ListBuffer

import xitrum.Action
import xitrum.scope.request.Params

class CometGetAction extends Action {
  override def postback {
    val channel       = param("channel")
    val lastTimestamp = tparam[Long]("lastTimestamp")

    val messages = Comet.getMessages(channel, lastTimestamp)

    // When there is no message, the connection is kept and the response will
    // be sent as soon as there a message arrives

    if (messages.isEmpty) {
      val messagePublished = (message: CometMessage) => {
        respondMessages(List(message))

        // Return true for Comet to automatically remove this listener.
        // With HTTP the reponse can only be sent once.
        true
      }

      Comet.addMessageListener(channel, messagePublished)

      // Avoid memory leak when messagePublished is never removed, e.g. no message is published
      addConnectionClosedListener(() => Comet.removeMessageListener(channel, messagePublished))
    } else {
      respondMessages(messages)
    }
  }

  //----------------------------------------------------------------------------

  private def respondMessages(messages: Iterable[CometMessage]) {
    val (timestamps, bodies) = messages.foldLeft((ListBuffer[Long](), ListBuffer[Params]())) { case ((ts, bs), m) =>
      ts.append(m.timestamp)
      bs.append(m.body)
      (ts, bs)
    }
    renderJson(Map("timestamps" -> timestamps.toList, "bodies" -> bodies.toList))
  }
}
