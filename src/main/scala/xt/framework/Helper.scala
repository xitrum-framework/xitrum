package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait Helper {
	// These variables will be set by middleware Failsafe

  var channel:  Channel          = _
	var request:  HttpRequest      = _
	var response: HttpResponse     = _
	var env:      Map[String, Any] = _

	var _params: java.util.Map[String, java.util.List[String]] = _

	// Equivalent to @xxx variables of Rails
  var _at: Map[String, Any] = _

	//----------------------------------------------------------------------------

  /**
   * Returns a singular element.
   */
  def param(key: String): Option[String] = {
    val values = _params.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): Option[List[String]] = {
    val values = _params.get(key)
    if (values == null) None else Some(JavaConversions.asBuffer[String](values).toList)
  }

  //----------------------------------------------------------------------------

  def at(key: String, value: Any) = _at.put(key, value)
  def at[T](key: String): T       = _at(key).asInstanceOf[T]
}
