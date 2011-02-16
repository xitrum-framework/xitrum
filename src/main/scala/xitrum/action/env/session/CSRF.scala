package xitrum.action.env.session

import java.util.UUID

import xitrum.action.Action
import xitrum.action.exception.InvalidCSRFToken

object CSRF {
  val TOKEN = "_csrf_token"
}

/**
 * SecureBase64 is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user data.
 */
trait CSRF {
  this: Action =>

  import CSRF._

  def serialize(value: Any): String = csrfToken + SecureBase64.serialize(value)

  def deserialize(string: String): Any = {
    val prefix = csrfToken
    if (!string.startsWith(prefix)) throw new InvalidCSRFToken

    val base64String = string.substring(prefix.length)
    SecureBase64.deserialize(base64String) match {
      case None       => throw new InvalidCSRFToken
      case Some(data) => data
    }
  }

  //----------------------------------------------------------------------------

  private def csrfToken: String = {
    sessiono(TOKEN) match {
      case Some(x) =>
        x.toString

      case None =>
        val y = UUID.randomUUID.toString
        session(TOKEN) = y
        y
    }
  }

  /* Saved for POST1, not used for now
  private def checkToken: Boolean = {
    // The token must be in the request body for more security
    val csrfTokenInRequest = param(TOKEN, bodyParams)
    bodyParams.remove(TOKEN)  // Cleaner for application developers when seeing access log
    val csrfTokenInSession = csrfToken
    csrfTokenInRequest == csrfTokenInSession
  }
  */
}
