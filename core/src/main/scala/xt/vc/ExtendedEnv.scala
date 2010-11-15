package xt.vc

import xt.vc.env._
import xt.vc.env.session.SessionRestorer

import java.util.{Map => JMap, LinkedHashMap => JLinkedHashMap, List => JList}

import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._

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

  lazy val response = new DefaultHttpResponse(HTTP_1_1, OK)

  lazy val cookies = new Cookies(request)

  lazy val session = SessionRestorer.restore(this)

  lazy val at = new At
}
