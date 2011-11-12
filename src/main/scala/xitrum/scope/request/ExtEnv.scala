package xitrum.scope.request

import xitrum.Config
import xitrum.Action
import xitrum.scope.session.CSRF

trait ExtEnv extends RequestEnv with ParamAccess with CSRF {
  this: Action =>

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  lazy val at = new At

  // Avoid encoding, decoding when cookies/session is not touched by the application
  private var sessionTouched = false
  private var cookiesTouched = false

  lazy val session = {
    sessionTouched = true
    Config.sessionStore.restore(this)
  }

  lazy val cookies = {
    cookiesTouched = true
    new Cookies(request)
  }

  def sessiono[T](key: String): Option[T] = session.get(key).map(_.asInstanceOf[T])

  def prepareWhenRespond {
    if (sessionTouched) Config.sessionStore.store(session, this)
    if (cookiesTouched) cookies.setCookiesWhenRespond(this)
  }

  def resetSession {
    session.clear
    cookies.clear  // This will clear the session cookie
  }
}
