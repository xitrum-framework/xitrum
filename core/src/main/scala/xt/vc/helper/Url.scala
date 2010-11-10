package xt.vc.helper

import xt.vc.Helper

import java.net.InetSocketAddress

trait Url {
  this: Helper =>

  /** @param name Controller#action or action */
  def urlFor(name: String, params: Any*): String = {
    "TODO"
  }
}
