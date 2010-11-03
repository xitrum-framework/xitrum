package xt.middleware

import xt.framework.Controller

import scala.collection.mutable.HashMap

/**
 * Used by Netty handler, middlewares, and controllers to share data for both
 * request and response pipeline processing.
 *
 * Design decision: HashMap[String, Any] is not used directly so that strong type
 * checking can be done in the spirit of Scala.
 */
class Env extends HashMap[String, Any] {
	/**
	 * By default the Netty handler will send the response to the client. This
	 * operataion will end the HTTP request. If you want to send the response
	 * yourself, set this to false.
	 */
	var autoRespond = true

  var controller: Controller = _
}
