package colinh

import org.jboss.netty.handler.codec.http.HttpMethod._

import st.server.Server
import st.framework.STApp
import st.middleware.{App, Static,
                      ParamsParser,
                      MethodOverride, Dispatcher, Failsafe, Squeryl}

object Http {
  private val routes =
    (GET,   "/",                    "Articles#index")  ::
    (GET,   "/articles/page/:page", "Articles#index")  ::
    (GET,   "/articles/make",       "Articles#make")   ::
    (POST,  "/articles",            "Articles#create") ::
    (GET,   "/articles/:id",        "Articles#show")   ::
    (GET,   "/articles/:id/edit",   "Articles#edit")   :: Nil

  private val errorRoutes = Map(
    "404" -> "Errors#error404",
    "500" -> "Errors#error500")

  private val controllerPaths = List("colinh.controller")

  def main(args: Array[String]) {
    var app: App = new STApp
    app = Failsafe.wrap(app)  // Failsafe should be the last in the middleware chain
    app = Squeryl.wrap(app)
    app = Dispatcher.wrap(app, routes, errorRoutes, controllerPaths)
    app = MethodOverride.wrap(app)
    app = ParamsParser.wrap(app)
    app = Static.wrap(app)

    val s = new Server(app)
    s.start
  }
}
