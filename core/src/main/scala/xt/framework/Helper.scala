package xt.framework

import scala.collection.mutable.Map
import scala.collection.JavaConversions

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xt._

trait Helper extends Logger {
  // These variables will be set by middleware Failsafe or
  // when an action renders a view, or when a view renders another view

  var channel:  Channel          = _
  var request:  HttpRequest      = _
  var response: HttpResponse     = _
  var env:      Map[String, Any] = _

  protected var paramsMap: java.util.Map[String, java.util.List[String]] = _

  // Equivalent to @xxx variables of Rails
  protected var atMap: Map[String, Any] = _

  /**
   * Sets references from another helper. Not cloning because we want for example
   * if something is added in this atMap, it will be reflected at other's atMap.
   */
  def setRefs(other: Helper) {
    setRefs(other.channel, other.request, other.response, other.env, other.paramsMap, other.atMap)
  }

  def setRefs(channel: Channel, request: HttpRequest, response: HttpResponse,
      env: Map[String, Any],
      paramsMap: java.util.Map[String, java.util.List[String]],
      atMap: Map[String, Any]) {
    this.channel   = channel
    this.request   = request
    this.response  = response
    this.env       = env
    this.paramsMap = paramsMap
    this.atMap     = atMap
  }

  //----------------------------------------------------------------------------

  /**
   * Returns a singular element.
   */
  def param(key: String): String = {
    if (paramsMap.containsKey(key))
      paramsMap.get(key).get(0)
    else
      throw new xt.middleware.Failsafe.MissingParam(key)
  }

  def paramo(key: String): Option[String] = {
    val values = paramsMap.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): List[String] = {
    if (paramsMap.containsKey(key))
      JavaConversions.asBuffer[String](paramsMap.get(key)).toList
    else
      throw new xt.middleware.Failsafe.MissingParam(key)
  }

  def paramso(key: String): Option[List[String]] = {
    val values = paramsMap.get(key)
    if (values == null) None else Some(JavaConversions.asBuffer[String](values).toList)
  }

  //----------------------------------------------------------------------------

  def at(key: String, value: Any) = atMap.put(key, value)
  def at[T](key: String): T       = atMap(key).asInstanceOf[T]

  //----------------------------------------------------------------------------

  /**
   * Renders a view without layout.
   *
   * csasOrAs: String in the pattern "Articles#index" or "index"
   */
  def render(csasOrAs: String) = Scalate.render(csasOrAs, this)
}
