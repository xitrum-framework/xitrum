package xitrum.action.env

import java.util.{Map => JMap, LinkedHashMap => JLinkedHashMap, List => JList}

import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpVersion, HttpHeaders}
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.action.Action
import xitrum.action.env.session.CSRF

trait ExtEnv extends Env with ParamAccess with CSRF {
  this: Action =>

  lazy val allParams = {
    val ret = new JLinkedHashMap[String, JList[String]]()
    // The order is important because we want the later to overwrite the former
    ret.putAll(uriParams)
    ret.putAll(bodyParams)
    ret.putAll(pathParams)
    ret
  }

  /** The default response is empty 200 OK */
  lazy val response = {
    val ret = new DefaultHttpResponse(HTTP_1_1, OK)
    HttpHeaders.setContentLength(ret, 0)
    ret
  }

  // Avoid encoding, decoding when cookies/session is not touched by the application
  private[this] var _cookiesTouched = false
  private[this] var _sessionTouched = false

  def isCookiesTouched = _cookiesTouched
  def isSessionTouched = _sessionTouched

  lazy val cookies = {
    _cookiesTouched = true
    new Cookies(request)
  }

  lazy val session = {
    _sessionTouched = true
    Config.sessionStore.restore(this)
  }

  def sessiono[T](key: String): Option[T] = {
    if (session.contains(key)) {
      Some(session[T](key))
    } else {
      None
    }
  }

  lazy val at = new At

  def prepareWhenRespond {
    if (isSessionTouched) {
      clearFlashWhenRespond
      Config.sessionStore.store(session, this)
    }

    if (isCookiesTouched) {
      cookies.setCookiesWhenRespond(this)
    }
  }
}
