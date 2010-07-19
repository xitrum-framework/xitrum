package xt.middleware

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait App {
  def handle(req: HttpRequest, res: HttpResponse)
}
