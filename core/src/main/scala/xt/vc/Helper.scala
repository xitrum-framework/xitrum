package xt.vc

import xt._
import xt.vc.helper._

import java.net.InetSocketAddress
import scala.collection.JavaConversions
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait Helper extends Env with Logger {
  /**
   * @return IP of the HTTP client, X-Forwarded-For is supported
   *
   * See http://en.wikipedia.org/wiki/X-Forwarded-For
   *
   * TODO: see http://github.com/pepite/Play--Netty/blob/master/src/play/modules/netty/PlayHandler.java
   *
   * TODO: inetSocketAddress can be Inet4Address or Inet6Address
   * See java.net.preferIPv6Addresses
   */
  lazy val remoteIp = {
    val inetSocketAddress = lastUpstreamHandlerCtx.getChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
    val ip = inetSocketAddress.getAddress.getHostAddress
    ip
  }

  //----------------------------------------------------------------------------

  class MissingParam(key: String) extends Throwable(key)

  /**
   * Returns a singular element.
   */
  def param(key: String): String = {
    if (allParams.containsKey(key))
      allParams.get(key).get(0)
    else
      throw new MissingParam(key)
  }

  def paramo(key: String): Option[String] = {
    val values = allParams.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): List[String] = {
    if (allParams.containsKey(key))
      JavaConversions.asBuffer[String](allParams.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def paramso(key: String): Option[List[String]] = {
    val values = allParams.get(key)
    if (values == null) None else Some(JavaConversions.asBuffer[String](values).toList)
  }

  //----------------------------------------------------------------------------

  /**
   * @param name Controller#action or action
   */
  def urlFor(name: String, params: Any*): String = {
    "TODO"
  }

  //----------------------------------------------------------------------------

  /**
   * Renders a template without layout.
   *
   * csasOrAs: Controller#action or action
   */
  def render(csasOrAs: String) = Scalate.render(csasOrAs, this)
}
