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
    sessiono(TOKEN) match {
      case Some(x) => x

      case None =>
        val y = UUID.randomUUID.toString
        session(TOKEN) = y
        y
    }
  }

  def checkToken = {
    // The token must be in the request body for more security
    val csrfTokenInRequest = param(TOKEN, bodyParams)
    bodyParams.remove(TOKEN)  // Cleaner for application developers when seeing access log
    val csrfTokenInSession = csrfToken
    csrfTokenInRequest == csrfTokenInSession
  }
}
