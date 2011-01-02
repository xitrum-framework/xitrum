package xt.vc.env

import java.util.{Map => JMap, LinkedHashMap => JLinkedHashMap, List => JList}

import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpVersion, HttpHeaders}
import HttpResponseStatus._
import HttpVersion._

import xt.Controller
import xt.vc.env.session.SessionRestorer

trait ExtendedEnv extends Env {
  this: Controller =>

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

  // TODO: avoid encoding, decoding when cookies/session is not touched by the application
  lazy val cookies = new Cookies(request)
  lazy val session = SessionRestorer.restore(this)

  lazy val at = new At
}
