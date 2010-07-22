package colinh

import org.jboss.netty.handler.codec.http.HttpMethod._

import xt.server.Server
import xt.framework.RoutedApp
import xt.middleware.{Params, Route}

object Http {
  private val routes =
    (GET,  "/",                  "colinh.controller.Articles#index")  ::
    (GET,  "/articles/:id",      "colinh.controller.Articles#show")   ::
    (GET,  "/articles/:id/edit", "colinh.controller.Articles#edit")   ::
    (null, "404",                "colinh.controller.Errors#error404") ::
    (null, "500",                "colinh.controller.Errors#error500") :: Nil

  private val controllerPaths = List("colinh.controller")
  private val viewPaths       = List("colinh.view")


  def main(args: Array[String]) {
    val a1 = new RoutedApp
    val a2 = Route.wrap(a1, routes, controllerPaths, viewPaths)
    val a3 = Params.wrap(a2)

    val s = new Server(a3)
    s.start
  }
}
