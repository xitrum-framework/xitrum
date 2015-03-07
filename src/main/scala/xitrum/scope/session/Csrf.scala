package xitrum.scope.session

import java.util.UUID

import xitrum.Action
import xitrum.util.{DefaultsTo, SeriDeseri}

/**
 * SeriDeseri's to/fromSecureUrlSafeBase64 is for preventing a user to mess with
 * his own data to cheat the server. CSRF is for preventing a user to fake other
 * user's data.
 */
object Csrf {
  val TOKEN         = "csrf-token"
  val X_CSRF_HEADER = "X-CSRF-Token"

  def isValidToken(action: Action): Boolean = {
    // The token must be in the request body for more security
    val bodyTextParams = action.handlerEnv.bodyTextParams
    val headers        = action.handlerEnv.request.headers
    val tokenInRequest = Option(headers.get(X_CSRF_HEADER)).getOrElse(action.param(TOKEN, bodyTextParams))

    // Cleaner for application developers when seeing access log
    bodyTextParams.remove(TOKEN)

    val tokenInSession = action.antiCsrfToken
    tokenInRequest == tokenInSession
  }

  /**
   * For encrypting things that need to embed the anti-CSRF token for more security.
   * (For example when using with GET requests, which does not include the token.)
   * Otherwise you should use SeriDeseri's to/fromSecureUrlSafeBase64 for shorter
   * result.
   */
  def encrypt(action: Action, any: Any): String =
    action.antiCsrfToken + SeriDeseri.toSecureUrlSafeBase64(any)

  def decrypt[T](action: Action, string: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    val prefix = action.antiCsrfToken
    if (!string.startsWith(prefix)) {
      None
    } else {
      val base64String = string.substring(prefix.length)
      SeriDeseri.fromSecureUrlSafeBase64[T](base64String)(e, m)
    }
  }
}

trait Csrf {
  this: Action =>

  import Csrf._

  def antiCsrfToken: String = {
    sessiono(TOKEN) match {
      case Some(x) =>
        x

      case None =>
        val y = UUID.randomUUID().toString
        session(TOKEN) = y
        y
    }
  }

  lazy val antiCsrfMeta  = <meta name={TOKEN} content={antiCsrfToken} />

  lazy val antiCsrfInput = <input type="hidden" name={TOKEN} value={antiCsrfToken} />
}
