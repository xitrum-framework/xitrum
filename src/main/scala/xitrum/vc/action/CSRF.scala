package xitrum.vc.action

import java.util.UUID
import xitrum.Action

object CSRF {
  val TOKEN = "_csrf_token"
}

trait CSRF {
  this: Action =>

  import CSRF._

  def csrfToken = {
    val x = session(TOKEN)
    if (x.isEmpty) {
      val y = UUID.randomUUID.toString
      session(TOKEN) = y
      y
    } else {
      x.get
    }
  }

  def checkToken = {
    val csrfTokenInRequest = param(TOKEN)
    val csrfTokenInSession = csrfToken
    csrfTokenInRequest == csrfTokenInSession
  }
}
