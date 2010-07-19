package xt.middleware

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

class RoutedApp extends App {
  def handle(req: HttpRequest, res: HttpResponse) {
    println(req)
  }
}
