package xitrum.util

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponse, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.Action

object NotModified {
  private val SECS_IN_A_YEAR = 60 * 60 * 24 * 365

  // SimpleDateFormat is locale dependent
  // Avoid the case when Xitrum is run on for example Japanese platform
  private val rfc2822 = {
    val ret = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    ret.setTimeZone(TimeZone.getTimeZone("GMT"))
    ret
  }

  // See PublicResourceServerAction, JSRoutesAction
  val serverStartupTimestamp        = System.currentTimeMillis
  val serverStartupTimestampRfc2822 = formatRfc2822(serverStartupTimestamp)

  def formatRfc2822(timestamp: Long) = rfc2822.format(timestamp)

  /** @return true if NOT_MODIFIED response has been sent */
  def respondIfNotModifidedSinceServerStart(action: Action) = {
    if (action.request.getHeader(IF_MODIFIED_SINCE) == serverStartupTimestampRfc2822) {
      action.response.setStatus(NOT_MODIFIED)
      action.respond
      true
    } else {
      false
    }
  }

  def setMaxAgeUntilNextServerRestart(response: HttpResponse) {
    response.setHeader(LAST_MODIFIED, serverStartupTimestampRfc2822)
    response.setHeader(CACHE_CONTROL, MAX_AGE + "=" + SECS_IN_A_YEAR)
  }
}
