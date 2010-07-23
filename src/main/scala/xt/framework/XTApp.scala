package xt.framework

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt.middleware.App

/**
 * This app should be put behind middlewares:
 * ParamsParser -> MethodOverride -> Dispatcher -> Failsafe
 */
class XTApp extends App {
  def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
    val controller = env("controller").asInstanceOf[Controller]
    val action     = env("action").asInstanceOf[Method]

    action.invoke(controller)
    response.setContent(ChannelBuffers.copiedBuffer("Hello", CharsetUtil.UTF_8))
  }
}
