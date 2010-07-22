package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

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
    def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
      env.get("controller") match {
        case Some(c) =>
          try {
            val controller = c.asInstanceOf[Controller]
            setHelper(controller, env)
            app.call(req, res, env)
          } catch {
            case e1 =>
              try {
                // TODO: log
                e1.printStackTrace

                val controller = env("controller500").asInstanceOf[Controller]
                val action     = env("action500").asInstanceOf[Method]

                setHelper(controller, env)
                res.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                action.invoke(controller)
              } catch {
                case e2 =>
                  // TODO: log
                  e2.printStackTrace

                  res.setContent(ChannelBuffers.copiedBuffer("Internal Server Error", CharsetUtil.UTF_8))
              }
          }

        case None =>
          try {
            val controller = env("controller404").asInstanceOf[Controller]
            val action     = env("action404").asInstanceOf[Method]

            setHelper(controller, env)
            res.setStatus(HttpResponseStatus.NOT_FOUND)
            action.invoke(controller)
          } catch {
            case e =>
              // TODO: log
              e.printStackTrace

              res.setContent(ChannelBuffers.copiedBuffer("Not Found", CharsetUtil.UTF_8))
          }
      }
    }
  }

  private def setHelper(controller: Controller, env: Map[String, Any]) {
    // Set params
    val params = env("params").asInstanceOf[java.util.Map[String, java.util.List[String]]]
    controller.setParams(params)

    // Set at
    val at = new HashMap[String, Any]
    controller.setAt(at)
  }
}
