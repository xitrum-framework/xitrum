package xitrum.action

import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.Action
import xitrum.handler.up.Dispatcher

trait Redirect {
  this: Action =>

  def redirectTo(location: String, status: HttpResponseStatus = FOUND) {
    response.setStatus(status)
    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond
  }

  def redirectTo[T: Manifest] { redirectTo(urlFor[T]) }

  def redirectTo[T: Manifest](params: (String, Any)*) { redirectTo(urlFor[T](params:_*)) }

  //----------------------------------------------------------------------------

  private var postback = false

  def isPostback: Boolean = postback

  // Called by PostbackAction
  def setPostback(postback: Boolean) {
    this.postback = postback
  }

  def forward(actionClass: Class[_ <: Action], postback: Boolean) {
    val action = actionClass.newInstance
    action(handlerEnv)
    action.setPostback(isPostback)
    Dispatcher.dispatchWithFailsafe(action, postback)
  }
}
