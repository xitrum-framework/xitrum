package xitrum.routing

import java.lang.reflect.Method
import io.netty.handler.codec.http.HttpHeaders

import xitrum.Controller
import xitrum.exception.SessionExpired
import xitrum.util.SecureBase64
import xitrum.validator.ValidatorCaller

object PostbackController extends PostbackController

class PostbackController extends Controller {
  /** Route to this is automatically added by RouteCollector. */
  val postback = POST("/xitrum/postback/:*") {  // *: encryptedActionClassName
    setPostback(true)

    val secureActionClassName = param("*")
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
