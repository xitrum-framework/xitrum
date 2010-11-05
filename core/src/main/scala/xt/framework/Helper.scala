package xt.framework

import xt._
import xt.middleware.{Env, Session}

import java.net.InetSocketAddress
import scala.collection.JavaConversions
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait Helper extends Logger {
  // These variables will be set by middleware Failsafe or
  // when an action renders a view, or when a view renders another view.
  //
  // They are public so that they can be accessed from views.

  var channel:  Channel      = _
  var request:  HttpRequest  = _
  var response: HttpResponse = _
  var env:      Env          = _

  var at:       HelperAt     = _

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
    val inetSocketAddress = channel.getRemoteAddress.asInstanceOf[InetSocketAddress]
    val ip = inetSocketAddress.getAddress.getHostAddress
    ip
  }

  /**
   * Sets references from another helper. Not cloning because we want for example
   * if something is added in this atMap, it will be reflected at other's atMap.
   */
  def setRefs(other: Helper) {
    setRefs(other.channel, other.request, other.response, other.env, other.at)
  }

  def setRefs(channel:   Channel,
              request:   HttpRequest,
              response:  HttpResponse,
              env:       Env,
              at:        HelperAt) {
    this.channel   = channel
    this.request   = request
    this.response  = response
    this.env       = env
    this.at        = at
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a singular element.
   */
  def param(key: String): String = {
    val m = env.params
    if (m.containsKey(key))
      m.get(key).get(0)
    else
      throw new xt.middleware.Failsafe.MissingParam(key)
  }

  def paramo(key: String): Option[String] = {
    val values = env.params.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): List[String] = {
    val m = env.params
    if (m.containsKey(key))
      JavaConversions.asBuffer[String](m.get(key)).toList
    else
      throw new xt.middleware.Failsafe.MissingParam(key)
  }

  def paramso(key: String): Option[List[String]] = {
    val values = env.params.get(key)
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
