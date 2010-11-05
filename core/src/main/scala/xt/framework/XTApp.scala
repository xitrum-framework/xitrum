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
  def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
    val controller = env.controller
    val action     = env.action

    val at = new HelperAt
    controller.setRefs(channel, request, response, env, at)

    val passed = controller.beforeFilters.forall(filter => {
      val onlyActions = filter._2
      if (onlyActions.isEmpty) {
        val exceptActions = filter._3
        if (!exceptActions.contains(action)) {
          val method = filter._1
          method.invoke(controller).asInstanceOf[Boolean]
        }	else true
      } else {
        if (onlyActions.contains(action)) {
          val method = filter._1
          method.invoke(controller).asInstanceOf[Boolean]
        } else true
      }
    })
    if (passed) action.invoke(controller)
  }
}
