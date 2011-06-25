package xitrum.action.routing

import org.jboss.netty.handler.codec.http.HttpHeaders

import xitrum.Action
import xitrum.annotation.POST
import xitrum.scope.PathInfo
import xitrum.scope.session.CSRF
import xitrum.exception.InvalidCSRFToken
import xitrum.validation.ValidatorCaller

object PostbackAction {
  val POSTBACK_PREFIX  = "/xitrum/postback/"  // Postback URLs are in the form POSTBACK_PREFIX + encryptedActionClassName
}

/** Route to this action is automatically added by RouteCollector. */
class PostbackAction extends Action {
  override def execute {
    isPostback = true

    val encoded               = pathInfo.encoded
    val secureActionClassName = encoded.substring(PostbackAction.POSTBACK_PREFIX.length)

    var actionClassName: String = null
    try {
      actionClassName = CSRF.decrypt(this, secureActionClassName).asInstanceOf[String]
    } catch {
      case e: InvalidCSRFToken =>
        session.reset
        jsRenderCall("alert", "\"Session expired. Please refresh your browser.\"")
        return

      case other =>
        throw other
    }

    if (ValidatorCaller.call(this)) {
      val actionClass = Class.forName(actionClassName).asInstanceOf[Class[Action]]
      henv.pathInfo   = new PathInfo(actionClass.getName)  // /xitrum/postback/blahblah is meaningless => Use the destination class name
      forward(actionClass, true)
    } else {
      // Flash the default error message if the response is empty (the validators did not respond anything)
      if (HttpHeaders.getContentLength(response, 0) == 0) jsFlash("Please check your input.")
    }
  }
}
