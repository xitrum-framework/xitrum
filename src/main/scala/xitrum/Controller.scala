package xitrum

import java.io.File
import scala.xml.Elem

import io.netty.channel.{ChannelFuture, ChannelFutureListener}

import xitrum.controller._
import xitrum.handler.up.Dispatcher
import xitrum.scope.request.ExtEnv
import xitrum.validator.{Validator, ValidatorInjector}
import xitrum.view.Renderer

trait Controller extends ExtEnv with RouteFactory with Logger with Net with Filter with BasicAuthentication with WebSocket with Redirect with UrlFor with Renderer with I18n {
  private var _pathPrefix = ""
  def pathPrefix(prefix: String) { _pathPrefix = prefix }
  def pathPrefix = _pathPrefix

  //----------------------------------------------------------------------------

  private var responded = false

  def isResponded = responded

  def respond = synchronized {
    if (responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double response", e)
      }
    } else {
      responded = true
      prepareWhenRespond
      channel.write(handlerEnv)
    }
  }

  //----------------------------------------------------------------------------

  // For Validators to use
  implicit val controller: Controller = this

  //----------------------------------------------------------------------------

  def addConnectionClosedListener(listener: () => Unit) {
    val dispatcher = channel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        listener()
      }
    })
  }
}
