package colinh

import org.jboss.netty.handler.codec.http.HttpMethod._

import xt.server.Server
import xt.framework.RoutedApp
import xt.middleware.Params

object Http {
  private val routes =
    (GET, "/",                  "Articles#index") ::
    (GET, "/articles/:id",      "Articles#show")  ::
    (GET, "/articles/:id/edit", "Articles#edit")  :: Nil

  def main(args: Array[String]) {
    val a1 = new RoutedApp
    val a2 = Params.wrap(a1)

    val s = new Server(a2)
    s.start
  }
}
