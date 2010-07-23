package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt.framework.Controller

/**
 * Catches error 500 and 404.
 * This middleware should be put behind Dispatcher.
 */
object Failsafe {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      env.get("controller") match {
        case Some(c) =>
          try {
            val controller = c.asInstanceOf[Controller]
            setHelper(controller, channel, request, response, env)
            app.call(channel, request, response, env)
          } catch {
            case e1 =>
              try {
                // TODO: log
                e1.printStackTrace

                val controller = env("controller500").asInstanceOf[Controller]
                val action     = env("action500").asInstanceOf[Method]

                setHelper(controller, channel, request, response, env)
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                action.invoke(controller)
              } catch {
                case e2 =>
                  // TODO: log
                  e2.printStackTrace

                  response.setContent(ChannelBuffers.copiedBuffer("Internal Server Error", CharsetUtil.UTF_8))
              }
          }

        case None =>
          try {
            val controller = env("controller404").asInstanceOf[Controller]
            val action     = env("action404").asInstanceOf[Method]

            setHelper(controller, channel, request, response, env)
            response.setStatus(HttpResponseStatus.NOT_FOUND)
            action.invoke(controller)
          } catch {
            case e =>
              // TODO: log
              e.printStackTrace

              response.setContent(ChannelBuffers.copiedBuffer("Not Found", CharsetUtil.UTF_8))
          }
      }
    }
  }

  private def setHelper(controller: Controller,
      channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
  	controller.request  = request
  	controller.response = response
  	controller.env      = env

    // Set params
    val params = env("params").asInstanceOf[java.util.Map[String, java.util.List[String]]]
    controller._params = params

    // Set at
    val at = new HashMap[String, Any]
    controller._at = at
  }
}
