package colinh

import org.jboss.netty.handler.codec.http.HttpMethod._

import xt.server.Server
import xt.framework.XTApp
import xt.middleware.{App, Static,
                      ParamsParser,
                      MethodOverride, Dispatcher, Failsafe, Squeryl}

object Http {
  private val routes =
    (GET,  "/",                  "Articles#index")  ::
    (GET,  "/articles/:id",      "Articles#show")   ::
    (GET,  "/articles/:id/edit", "Articles#edit")   ::
    (null, "404",                "Errors#error404") ::
    (null, "500",                "Errors#error500") :: Nil

  private val controllerPaths = List("colinh.controller")

  def main(args: Array[String]) {
    var app: App = new XTApp
    app = Squeryl.wrap(app, "postgresql")
    app = Failsafe.wrap(app)
    app = Dispatcher.wrap(app, routes, controllerPaths)
    app = MethodOverride.wrap(app)
    app = ParamsParser.wrap(app)
    app = Static.wrap(app)

    val s = new Server(app)
    s.start
  }
}
