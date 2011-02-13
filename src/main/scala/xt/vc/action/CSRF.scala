package xt.vc.action

import java.util.UUID
import xt.Action

trait CSRF {
  this: Action =>

  def csrfToken = {
    val x = session("_csrf_token")
    if (x.isEmpty) {
      val y = UUID.randomUUID.toString
      session("_csrf_token") = y
      y
    } else {
      x.get
    }
  }
}
