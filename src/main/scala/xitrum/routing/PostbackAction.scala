package xitrum.routing

import org.jboss.netty.handler.codec.http.HttpHeaders

import xitrum.Action
import xitrum.exception.SessionExpired
import xitrum.util.SecureBase64
import xitrum.validation.ValidatorCaller

object PostbackAction {
  val POSTBACK_PREFIX  = "/xitrum/postback/"  // Postback URLs are in the form POSTBACK_PREFIX + encryptedActionClassName
}

/** Route to this action is automatically added by RouteCollector. */
class PostbackAction extends Action {
  override def execute {
    setPostback(true)

    val encoded               = pathInfo.encoded
    val secureActionClassName = encoded.substring(PostbackAction.POSTBACK_PREFIX.length)

    SecureBase64.decrypt(secureActionClassName) match {
      case None => throw new SessionExpired

      case Some(obj) =>
        val actionClassName = obj.asInstanceOf[String]
        if (ValidatorCaller.call(this)) {
          val actionClass = Class.forName(actionClassName).asInstanceOf[Class[Action]]
          forward(actionClass, true)
        } else {
          // Flash the default error message if the response is empty (the validators did not respond anything)
          if (HttpHeaders.getContentLength(response, 0) == 0) jsRenderFlash("Please check your input.")
        }
    }
  }
}
