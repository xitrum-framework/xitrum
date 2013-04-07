package xitrum.routing

import java.io.Serializable
import org.jboss.netty.handler.codec.http.HttpMethod
import xitrum.Action

class SerializableRoute(
  // In
  val httpMethod: String, val compiledPattern: Seq[RouteToken],

  // Out
  val actionClass: String, val cacheSecs: Int
) extends Serializable
{
  def toRoute: Route = {
    new Route(
      new HttpMethod(httpMethod),
      compiledPattern, Class.forName(actionClass).asInstanceOf[Class[Action]],
      cacheSecs
    )
  }
}
