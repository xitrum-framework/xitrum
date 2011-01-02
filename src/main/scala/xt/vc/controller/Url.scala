package xt.vc.controller

import java.net.InetSocketAddress
import xt.Controller

trait Url {
  this: Controller =>

  /** @param name Controller#action or action */
  def urlFor(name: String, params: Any*): String = {
    "TODO"
  }
}
