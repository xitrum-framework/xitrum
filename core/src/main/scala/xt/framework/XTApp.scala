package xt.framework

import scala.collection.mutable.HashMap

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xt.middleware.{App, Env}

/**
 * This app should be put behind middlewares:
 * Static -> ParamsParser -> MethodOverride -> Dispatcher -> Failsafe -> XTApp
 */
class XTApp extends App {
  def call(remoteIp: String, channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
    val controller = env.controller
    val action     = env.action

    val atMap = new HashMap[String, Any]
    controller.setRefs(remoteIp, channel, request, response, env, atMap)

    val passed = controller.beforeFilters.forall(name_f => name_f._2())
    if (passed) action.invoke(controller)
  }
}
