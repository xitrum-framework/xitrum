package xitrum.routing

import java.lang.reflect.Method
import io.netty.handler.codec.http.HttpHeaders

import xitrum.Controller
import xitrum.exception.SessionExpired
import xitrum.util.SecureBase64
import xitrum.validator.ValidatorCaller

object PostbackController extends PostbackController {
  val POSTBACK_PREFIX = "/xitrum/postback/" // Postback URLs are in the form POSTBACK_PREFIX + encryptedActionClassName
}

class PostbackController extends Controller {
  import PostbackController._

  /** Route to this is automatically added by RouteCollector. */
  val postback = POST(POSTBACK_PREFIX + ":*") {
    setPostback(true)

    val encoded               = pathInfo.encoded
    val secureActionClassName = encoded.substring(POSTBACK_PREFIX.length)

    SecureBase64.decrypt(secureActionClassName) match {
      case None => throw new SessionExpired

      case Some(obj) =>
        val routeMethod = obj.asInstanceOf[Method]
        if (ValidatorCaller.call(this)) {
          // FIXME
          //forward(routeMethod, true)
        } else {
          // Flash the default error message if the response is empty (the validators did not respond anything)
          if (HttpHeaders.getContentLength(response, 0) == 0) jsRenderFlash("Please check your input.")
        }
    }
  }
}
