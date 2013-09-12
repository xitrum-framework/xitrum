package xitrum.scope.session

import java.util.UUID

import xitrum.Action
import xitrum.exception.InvalidAntiCsrfToken
import xitrum.util.SecureUrlSafeBase64

/**
 * SecureBase64 is for preventing a user to mess with his own data to cheat the server.
 * CSRF is for preventing a user to fake other user data.
 */
object Csrf {
  val TOKEN = "csrf-token"

  def isValidToken(action: Action): Boolean = {
    // The token must be in the request body for more security
    val bodyParams     = action.handlerEnv.bodyParams
    val tokenInRequest = action.param(TOKEN, bodyParams)

    // Cleaner for application developers when seeing access log
    bodyParams.remove(TOKEN)

    val tokenInSession = action.antiCsrfToken
    tokenInRequest == tokenInSession
  }

  /**
   * For encrypting things that need to embed the anti-CSRF token for more security.
   * (For example when using with GET requests, which does not include the token.)
   * Otherwise you should use SecureBase64.encrypt for shorter result.
   */
  def encrypt(action: Action, any: Any): String = action.antiCsrfToken + SecureUrlSafeBase64.encrypt(any)

  def decrypt(action: Action, string: String): Any = {
    val prefix = action.antiCsrfToken
    if (!string.startsWith(prefix)) throw new InvalidAntiCsrfToken

    val base64String = string.substring(prefix.length)
    SecureUrlSafeBase64.decrypt(base64String) match {
      case None       => throw new InvalidAntiCsrfToken
      case Some(data) => data
    }
  }
}

trait Csrf {
  this: Action =>

  import Csrf._

  def antiCsrfToken: String = {
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
  lazy val antiCsrfMeta  = "<meta name=\"" + TOKEN + "\" content=\"" + antiCsrfToken + "\" />"

  // Use String instead of Scala XML to avoid generating this (</input>):
  // <input name="csrf-token" type="hidden" value="d1d50807-5a0a-4d42-830a-a01a3628f2c8"></input>
  lazy val antiCsrfInput = "<input type=\"hidden\" name=\"" + TOKEN + "\" value=\"" + antiCsrfToken + "\"/>"
}
