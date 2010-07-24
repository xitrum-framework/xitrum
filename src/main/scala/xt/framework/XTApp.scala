package xt.framework

import java.lang.reflect.Method
import scala.collection.mutable.Map

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xt.middleware.App

/**
 * This app should be put behind middlewares:
 * Static -> ParamsParser -> MethodOverride -> Dispatcher -> Failsafe -> XTApp
 */
class XTApp extends App {
  def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
    val controller = env("controller").asInstanceOf[Controller]
    val action     = env("action").asInstanceOf[Method]

    action.invoke(controller)
  }
}
