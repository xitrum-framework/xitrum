package xt.vc.env.session

import java.util.{UUID, HashMap => JMap, Set => JSet}
import org.jboss.netty.handler.codec.http.Cookie

import xt._

object SessionUtil {
  /** Take out the cookie that stores the session ID */
  def findSessionCookie(cookies: JSet[Cookie]): Option[Cookie] = {
    val iter = cookies.iterator
    while (iter.hasNext) {
      val cookie = iter.next
      if (cookie.getName == Config.sessionIdName) return Some(cookie)
    }
    None
  }
}
