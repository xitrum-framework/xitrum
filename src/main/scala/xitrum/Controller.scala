package xitrum

import io.netty.channel.{ChannelFuture, ChannelFutureListener}

import xitrum.controller._
import xitrum.scope.request.ExtEnv
import xitrum.view.Responder

trait Controller extends ExtEnv with ActionFactory with Logger with Net with Filter with BasicAuthentication with WebSocket with Redirect with UrlFor with Responder with I18n {
  var pathPrefix = ""

  implicit val currentController: Controller = this

  def addConnectionClosedListener(listener: => Unit) {
    channel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        listener
      }
    })
  }
}
