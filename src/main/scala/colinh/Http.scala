package colinh

import org.jboss.netty.handler.codec.http.HttpMethod._

import xt.server.Server
import xt.framework.RoutedApp
import xt.middleware.{App, ParamsParser, MethodOverride, Dispatcher, Failsafe}

object Http {
  private val routes =
    (GET,  "/",                  "Articles#index")  ::
    (GET,  "/articles/:id",      "Articles#show")   ::
    (GET,  "/articles/:id/edit", "Articles#edit")   ::
    (null, "404",                "Errors#error404") ::
    (null, "500",                "Errors#error500") :: Nil

  private val controllerPaths = List("colinh.controller")
  private val viewPaths       = List("colinh.view")

  def main(args: Array[String]) {
    var app: App = new RoutedApp
    app = Failsafe.wrap(app)
    app = Dispatcher.wrap(app, routes, controllerPaths, viewPaths)
    app = MethodOverride.wrap(app)
    app = ParamsParser.wrap(app)

    val s = new Server(app)
    s.start
  }
}
