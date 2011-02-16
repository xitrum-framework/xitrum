package xitrum.action.routing

import xitrum.action.Action
import xitrum.action.annotation.POST
import xitrum.action.env.PathInfo
import xitrum.action.validation.ValidatorCaller

/** Route to this action is automatically added by RouteCollector. */
class POST2Action extends Action {
  def execute {
    pathParams.remove("*")  // Remove noisy information

    val encoded                = pathInfo.encoded
    val securedActionClassName = encoded.substring(Routes.POST2_PREFIX.length)
    val actionClassName        = deserialize(securedActionClassName).asInstanceOf[String]
    val actionClass            = Class.forName(actionClassName).asInstanceOf[Class[Action]]

    if (ValidatorCaller.call(this)) {
      henv("pathInfo")   = new PathInfo(actionClass.getName)  // /xitrum/post2/blahblah is meaningless => Use the destination class name
      henv("bodyParams") = bodyParams                         // Set decrypted params before forwarding
      forward(actionClass)
    } else {
      // TODO: some validator may only has server side (no browser side), render the errors
    }
  }
}
