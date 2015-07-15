package xitrum.action

import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.Action
import xitrum.handler.inbound.Dispatcher

trait Redirect {
  this: Action =>

  /**
   * Example: redirectTo("https://google.com/"); status will be 302 FOUND
   *
   * Example: redirectTo("https://google.com/", HttpResponseStatus.MOVED_PERMANENTLY)
   *
   * See also forwardTo.
   */
  def redirectTo(location: String, status: HttpResponseStatus = FOUND): ChannelFuture = {
    response.setStatus(status)
    HttpHeaders.setHeader(response, LOCATION, location)
    respond()
  }

  /**
   * Example: redirectTo[AnotherActionClass]()
   *
   * Example: redirectTo[AnotherActionClass]("param1" -> value1, "param2" -> value2)
   *
   * See also forwardTo.
   */
  def redirectTo[T <: Action : Manifest](params: (String, Any)*): ChannelFuture = {
    redirectTo(url[T](params: _*))
  }

  /**
   * Example: redirectToThis()
   *
   * Example: redirectToThis("param1" -> value1, "param2" -> value2)
   *
   * Redirects back to the current action. See also forwardTo.
   */
  def redirectToThis(params: (String, Any)*): ChannelFuture = {
    redirectTo(url(params: _*))
  }

  //----------------------------------------------------------------------------

  /** Set to true by forwardTo. */
  var forwarding = false

  /**
   * Example: forwardTo[AnotherActionClass]()
   *
   * Tells another action to process the current request for the current action.
   * See also redirectTo.
   */
  def forwardTo[T <: Action : Manifest]() {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    forwardTo(actionClass)
  }

  def forwardTo(actionClass: Class[_ <:Action]) {
    forwarding = true
    Dispatcher.dispatch(actionClass, handlerEnv)
  }
}
