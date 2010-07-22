package xt.framework

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt.middleware.App

class RoutedApp extends App {
  def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
    // Decide controller and action
    val (c, a) = env.get("controller") match {
      case Some(controller) =>
        val action = env("action").asInstanceOf[Method]
        (controller, action)

      case None =>
        res.setStatus(HttpResponseStatus.NOT_FOUND)
        val controller404 = env("controller404")
        val action404     = env("action404").asInstanceOf[Method]
        (controller404, action404)
    }

    // Set params for controller
    val params = env("params").asInstanceOf[java.util.Map[String, java.util.List[String]]]
    val c2 = c.asInstanceOf[Controller]
    c2.setParams(params)

    // Set at for controller
    val at = new HashMap[String, Any]
    c2.setAt(at)

    try {
      a.invoke(c)
      res.setContent(ChannelBuffers.copiedBuffer("Hello", CharsetUtil.UTF_8))
    } catch {
      case e1 =>
        try {
          // TODO: log
          println(e1)

          val controller500 = env("controller500")
          val action500     = env("action500").asInstanceOf[Method]

          res.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          action500.invoke(controller500)
        } catch {
          case e2 =>
            // TODO: log
            println(e2)

            res.setContent(ChannelBuffers.copiedBuffer("Not found", CharsetUtil.UTF_8))
        }
    }
  }
}
