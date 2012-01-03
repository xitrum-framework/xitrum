package xitrum.controller

import java.lang.reflect.Method

import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.Controller
import xitrum.handler.up.Dispatcher
import xitrum.routing.{Route, Routes}

trait Redirect {
  this: Controller =>

  def redirectTo(location: String, status: HttpResponseStatus = FOUND) {
    response.setStatus(status)
    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond
  }

  def redirectTo(route: Route, params: (String, Any)*) { redirectTo(route.url(params: _*)) }

  //----------------------------------------------------------------------------

  private var postback = false

  def isPostback: Boolean = postback

  def setPostback(postback: Boolean) {
    this.postback = postback
  }
}
