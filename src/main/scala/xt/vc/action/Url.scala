package xt.vc.action

import java.net.InetSocketAddress
import xt.Action

trait Url {
  this: Action =>

  /** @param name Controller#action or action */
  def urlFor(name: String, params: Any*): String = {
    "TODO"
  }
}
