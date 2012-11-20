package xitrum.scope.request

import xitrum.{Config, Controller}
import xitrum.scope.session.CSRF

trait ExtEnv extends RequestEnv with ParamAccess with CSRF {
  this: Controller =>

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  lazy val at = new At

  // Avoid encoding, decoding when cookies/session is not touched by the application
  private var sessionTouched = false
  private var cookiesTouched = false

  /** To reset session: session.clear() */
  lazy val session = {
    sessionTouched = true
    Config.sessionStore.restore(this)
  }

  /** To reset all cookies, cannot simply call cookies.clear(), see Xitrum guide */
  lazy val cookies = {
    cookiesTouched = true
    new Cookies(request)
  }

  def sessiono[T](key: String): Option[T] = session.get(key).map(_.asInstanceOf[T])

  def setCookieAndSessionIfTouchedOnRespond() {
    if (sessionTouched)
      // cookies is typically touched here
      Config.sessionStore.store(session, this)

    if (cookiesTouched)
      cookies.setCookiesWhenRespond(this)
  }
}
