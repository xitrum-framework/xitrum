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
  val postback = POST("/xitrum/postback/:ecr") {
    setPostback(true)

    val encryptedControllerRouteName = param("ecr")
    SecureBase64.decrypt(encryptedControllerRouteName) match {
      case None => throw new SessionExpired

      case Some(obj) =>
        if (ValidatorCaller.call(this)) {
          val controllerRouteName = obj.asInstanceOf[String]
          val (_controller, route) = ControllerReflection.newControllerAndRoute(controllerRouteName)
          forward(route, true)
        } else {
          // Flash the default error message if the response is empty (the validators did not respond anything)
          if (HttpHeaders.getContentLength(response, 0) == 0) jsRenderFlash("Please check your input.")
        }
    }
  }
}
