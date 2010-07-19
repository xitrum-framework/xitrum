package xt.middleware

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

class R(app: App) extends App {
  def handle(req: HttpRequest, res: HttpResponse) {

    app.handle(req, res)
  }
}

class Route {
  def wrap(app: App) = new R(app)
}
