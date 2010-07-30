package st.framework

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import st.middleware.App

/**
 * This app should be put behind middlewares:
 * Static -> ParamsParser -> MethodOverride -> Dispatcher -> Failsafe -> XTApp
 */
class STApp extends App {
  def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
    val controller = env("controller").asInstanceOf[Controller]
    val action     = env("action").asInstanceOf[Method]

    // setRefs
    val paramsMap = env("params").asInstanceOf[java.util.Map[String, java.util.List[String]]]
    val atMap = new HashMap[String, Any]
    controller.setRefs(channel, request, response, env, paramsMap, atMap)

    action.invoke(controller)
  }
}
