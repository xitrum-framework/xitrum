package xt.framework

import xt.server.Handler

trait Controller extends Helper with ControllerFilter with ControllerRender {
  def disableAutoRespond {
    env.autoRespond = false
  }

  def respond {
    Handler.respond(channel, request, response)
  }
}
