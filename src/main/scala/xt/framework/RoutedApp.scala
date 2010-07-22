package xt.framework

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt.middleware.App

/**
 * This app should be put behind middlewares:
 * Param -> Route -> Failsafe
 */
class RoutedApp extends App {
  def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
    val controller = env("controller").asInstanceOf[Controller]
    val action = env("action").asInstanceOf[Method]

    action.invoke(controller)
    res.setContent(ChannelBuffers.copiedBuffer("Hello", CharsetUtil.UTF_8))
  }
}
