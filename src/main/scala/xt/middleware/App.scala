package xt.middleware

import scala.collection.mutable.Map
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait App {
  def call(request: HttpRequest, response: HttpResponse, env: Map[String, Any])
}
