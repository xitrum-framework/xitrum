package xitrum.action

import java.lang.reflect.Method

import org.jboss.netty.channel.ChannelFuture

import org.jboss.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.{Action, Config}
import xitrum.handler.up.Dispatcher

trait Redirect {
  this: Action =>

  /** See also forwardTo. */
  def redirectTo(location: String, status: HttpResponseStatus = FOUND): ChannelFuture = {
    response.setStatus(status)
    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond()
  }

  /** See also forwardTo. */
  def redirectTo[T <: Action : Manifest](params: (String, Any)*): ChannelFuture = {
    redirectTo(url[T](params: _*))
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
    forwarding       = true
    handlerEnv.route = Config.routes.reverseMappings(actionClass)
    Dispatcher.dispatch(actionClass, handlerEnv)
  }
}
