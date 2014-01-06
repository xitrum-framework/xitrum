package xitrum

import xitrum.routing.RouteCollection

object Routes {
  def collect(): RouteCollection = Config.routes
}

