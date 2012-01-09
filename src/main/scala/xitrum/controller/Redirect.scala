package xitrum.controller

import java.lang.reflect.Method

import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.Controller
import xitrum.handler.up.Dispatcher

trait Redirect {
  this: Controller =>

  def redirectTo(location: String, status: HttpResponseStatus = FOUND) {
    response.setStatus(status)
    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond()
  }

  def redirectTo(action: Action, params: (String, Any)*) { redirectTo(action.url(params: _*)) }

  //----------------------------------------------------------------------------

  private var postback = false

  def isPostback: Boolean = postback

  def setPostback(postback: Boolean) {
    this.postback = postback
  }

  def forward(action: Action, postback: Boolean) {
    Dispatcher.dispatchWithFailsafe(action, handlerEnv, postback)
  }
}
