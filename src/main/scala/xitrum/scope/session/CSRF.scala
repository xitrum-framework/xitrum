package xitrum.scope.session

import java.util.UUID

import xitrum.Controller
import xitrum.exception.InvalidAntiCSRFToken
import xitrum.util.SecureBase64

/**
 * SecureBase64 is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user data.
 */
object CSRF {
  val TOKEN = "csrf-token"

  def isValidToken(controller: Controller): Boolean = {
    // The token must be in the request body for more security
    val tokenInRequest = controller.param(TOKEN, controller.bodyParams)
    // Cleaner for application developers when seeing access log
    controller.bodyParams.remove(TOKEN)
    val tokenInSession = controller.antiCSRFToken
    tokenInRequest == tokenInSession
  }

  /**
   * For encrypting things that need to embed the anti-CSRF token for more security.
   * (For example when using with GET requests, which does not include the token.)
   * Otherwise you should use SecureBase64.encrypt for shorter result.
   */
  def encrypt(controller: Controller, value: Any): String = controller.antiCSRFToken + SecureBase64.encrypt(value)

  def decrypt(controller: Controller, string: String): Any = {
    val prefix = controller.antiCSRFToken
    if (!string.startsWith(prefix)) throw new InvalidAntiCSRFToken

    val base64String = string.substring(prefix.length)
    SecureBase64.decrypt(base64String) match {
      case None       => throw new InvalidAntiCSRFToken
      case Some(data) => data
    }
  }
}

trait CSRF {
  this: Controller =>

  import CSRF._

  def antiCSRFToken: String = {
    sessiono(TOKEN) match {
      case Some(x) =>
        x.toString

      case None =>
        val y = UUID.randomUUID().toString
        session(TOKEN) = y
        y
    }
  }

  // Use String instead of Scala XML to avoid generating this (</meta>):
  // <meta name="csrf-token" content="d1d50807-5a0a-4d42-830a-a01a3628f2c8"></meta>
  lazy val antiCSRFMeta  = "<meta name=\"" + TOKEN + "\" content=\"" + antiCSRFToken + "\" />"

  // Use String instead of Scala XML to avoid generating this (</input>):
  // <input name="csrf-token" type="hidden" value="d1d50807-5a0a-4d42-830a-a01a3628f2c8"></input>
  lazy val antiCSRFInput = "<input type=\"hidden\" name=\"" + TOKEN + "\" value=\"" + antiCSRFToken + "\"/>"
}
