package xitrum.action.routing

import org.jboss.netty.handler.codec.http.HttpHeaders

import xitrum.action.Action
import xitrum.action.annotation.POST
import xitrum.action.env.PathInfo
import xitrum.action.exception.InvalidCSRFToken
import xitrum.action.validation.ValidatorCaller

/** Route to this action is automatically added by RouteCollector. */
class POST2Action extends Action {
  def execute {
    isPost2 = true

    pathParams.remove("*")  // Remove noisy information

    val encoded                = pathInfo.encoded
    val securedActionClassName = encoded.substring(Routes.POST2_PREFIX.length)

    var actionClassName: String = null
    try {
      actionClassName = deserialize(securedActionClassName).asInstanceOf[String]
    } catch {
      case e: InvalidCSRFToken =>
        session.reset
        jsRenderCall("alert", "\"Session expired. Please refresh your browser.\"")
        return

      case other =>
        throw other
    }

    val actionClass = Class.forName(actionClassName).asInstanceOf[Class[Action]]
    if (ValidatorCaller.call(this)) {
      henv.pathInfo   = new PathInfo(actionClass.getName)  // /xitrum/post2/blahblah is meaningless => Use the destination class name
      henv.bodyParams = bodyParams                         // Set decrypted params before forwarding
      forward(actionClass)
    } else {
      // Flash the default error message if the response is empty (the validators did not respond anything)
      if (HttpHeaders.getContentLength(response, 0) == 0) jsFlash("Please check your input.")
    }
  }
}
