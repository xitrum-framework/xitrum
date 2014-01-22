package xitrum.action

import java.lang.reflect.Method

import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.{Action, Config}
import xitrum.handler.inbound.Dispatcher

trait Redirect {
  this: Action =>

  /** See also forwardTo. */
  def redirectTo(location: String, status: HttpResponseStatus = FOUND): ChannelFuture = {
    response.setStatus(status)
    HttpHeaders.setHeader(response, LOCATION, location)
    respond()
  }

  /** See also forwardTo. */
  def redirectTo[T <: Action : Manifest](params: (String, Any)*): ChannelFuture = {
    redirectTo(url[T](params: _*))
  }

  def redirectToThis(params: (String, Any)*): ChannelFuture = {
    redirectTo(url(params: _*))
  }

  //----------------------------------------------------------------------------

  /** Set to true by forwardTo. */
  var forwarding = false

  /**
   * Tells another action to process the current request for the current action.
   * See also redirectTo.
   */
  def forwardTo[T <: Action : Manifest]() {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    forwarding = true
    Dispatcher.dispatch(actionClass, handlerEnv)
  }
}
