package xitrum.action.routing

import org.jboss.netty.handler.codec.http.HttpHeaders

import xitrum.action.Action
import xitrum.action.annotation.POST
import xitrum.action.env.PathInfo
import xitrum.action.env.session.CSRF
import xitrum.action.exception.InvalidCSRFToken
import xitrum.action.validation.ValidatorCaller

object PostbackAction {
  val POSTBACK_PREFIX  = "/xitrum/postback/"  // Postback URLs are in the form POSTBACK_PREFIX + encryptedActionClassName
}

/** Route to this action is automatically added by RouteCollector. */
class PostbackAction extends Action {
  override def execute {
    isPostback = true

    pathParams.remove("*")  // Remove noisy information

    val encoded                = pathInfo.encoded
    val securedActionClassName = encoded.substring(PostbackAction.POSTBACK_PREFIX.length)

    var actionClassName: String = null
    try {
      actionClassName = CSRF.decrypt(this, securedActionClassName).asInstanceOf[String]
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
      henv.pathInfo   = new PathInfo(actionClass.getName)  // /xitrum/postback/blahblah is meaningless => Use the destination class name
      henv.bodyParams = bodyParams                         // Set decrypted params before forwarding
      forward(actionClass, true)
    } else {
      // Flash the default error message if the response is empty (the validators did not respond anything)
      if (HttpHeaders.getContentLength(response, 0) == 0) jsFlash("Please check your input.")
    }
  }
}
