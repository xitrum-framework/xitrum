package xt.vc.controller

import xt.Controller

import java.net.InetSocketAddress

trait Url {
  this: Controller =>

  /** @param name Controller#action or action */
  def urlFor(name: String, params: Any*): String = {
    "TODO"
  }
}
