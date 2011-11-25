package xitrum.scope.session

import java.util.UUID

import xitrum.Action
import xitrum.exception.InvalidAntiCSRFToken
import xitrum.util.SecureBase64

/**
 * SecureBase64 is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user data.
 */
object CSRF {
  val TOKEN = "antiCSRFToken"

  def isValidToken(action: Action): Boolean = {
    // The token must be in the request body for more security
    val tokenInRequest = action.param(TOKEN, action.bodyParams)
    // Cleaner for application developers when seeing access log
    action.bodyParams.remove(TOKEN)
    val tokenInSession = action.antiCSRFToken
    tokenInRequest == tokenInSession
  }

  /**
   * For encrypting things that need to embed the anti-CSRF token for more security.
   * (For example when using with GET requests, which does not include the token.)
   * Otherwise you should use SecureBase64.encrypt for shorter result.
   */
  def encrypt(action: Action, value: Any): String = action.antiCSRFToken + SecureBase64.encrypt(value)

  def decrypt(action: Action, string: String): Any = {
    val prefix = action.antiCSRFToken
    if (!string.startsWith(prefix)) throw new InvalidAntiCSRFToken

    val base64String = string.substring(prefix.length)
    SecureBase64.decrypt(base64String) match {
      case None       => throw new InvalidAntiCSRFToken
      case Some(data) => data
    }
  }
}

trait CSRF {
  this: Action =>

  import CSRF._

  def antiCSRFToken: String = {
    sessiono(TOKEN) match {
      case Some(x) =>
        x.toString

      case None =>
        val y = UUID.randomUUID.toString
        session(TOKEN) = y
        y
    }
  }

  lazy val antiCSRFMeta  = <meta name={CSRF.TOKEN} content={antiCSRFToken} />

  lazy val antiCSRFInput = <input type="hidden" name={CSRF.TOKEN} value={antiCSRFToken} />
}
