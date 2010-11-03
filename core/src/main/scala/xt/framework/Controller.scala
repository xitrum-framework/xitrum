package xt.framework

import xt.server.Handler

trait Controller extends Helper with ControllerFilter with ControllerRender {
  def ignoreResponse {
    Handler.ignoreResponse(env)
  }

  def respond {
    Handler.respond(channel, request, response)
  }
}
