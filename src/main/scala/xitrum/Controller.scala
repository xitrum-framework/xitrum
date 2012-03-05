package xitrum

import java.io.File
import scala.xml.Elem

import io.netty.channel.{ChannelFuture, ChannelFutureListener}

import xitrum.controller._
import xitrum.handler.up.Dispatcher
import xitrum.scope.request.ExtEnv
import xitrum.view.Responder

trait Controller extends ExtEnv with ActionFactory with Logger with Net with Filter with BasicAuthentication with WebSocket with Redirect with UrlFor with Responder with I18n {
  var pathPrefix = ""

  implicit val currentController: Controller = this

  //----------------------------------------------------------------------------

  private var responded = false

  def isResponded = responded

  def respond() {
    if (responded) {
      // Double response error
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double response", e)
      }
    } else {
      responded = true
      prepareWhenRespond()
      channel.write(handlerEnv)
    }
  }

  //----------------------------------------------------------------------------

  def addConnectionClosedListener(listener: () => Unit) {
    val dispatcher = channel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        listener()
      }
    })
  }
}
