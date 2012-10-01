package xitrum.controller

import java.lang.reflect.Method

import org.jboss.netty.channel.ChannelFuture

import org.jboss.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import HttpHeaders.Names.LOCATION
import HttpResponseStatus.FOUND

import xitrum.Controller
import xitrum.handler.up.Dispatcher

trait Redirect {
  this: Controller =>

  def redirectTo(location: String, status: HttpResponseStatus = FOUND): ChannelFuture = {
    response.setStatus(status)
    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond()
  }

  def redirectTo(action: Action, params: (String, Any)*): ChannelFuture = { redirectTo(action.url(params: _*)) }
}
