package xt.framework

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}
import scala.collection.mutable.Map

import xt.middleware.App

class RoutedApp extends App {
  def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
    println(req, env)
  }
}
