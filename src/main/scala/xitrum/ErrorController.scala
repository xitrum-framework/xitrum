package xitrum

import xitrum.controller.Action

trait ErrorController {
  this: Controller =>

  def error404: Action
  def error500: Action
}
