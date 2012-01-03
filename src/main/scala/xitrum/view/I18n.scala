package xitrum.view

import xitrum.Controller

trait I18n {
  this: Controller =>

  def t(key: String, args: Any*): String = {
    val msg = key
    msg.format(args)
  }
}
