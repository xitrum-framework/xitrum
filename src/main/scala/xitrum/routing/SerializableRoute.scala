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
  /** @return A new route with the prefix */
  def addPrefix(prefix: String): SerializableRoute = {
    val routeToken = RouteToken(prefix, false, None)
    val firstToken = compiledPattern.head

    val newCompiledPattern =
      if (firstToken.value.isEmpty)
        Seq(routeToken)
      else
        Seq(routeToken) ++ compiledPattern

    new SerializableRoute(httpMethod, Seq(routeToken) ++ compiledPattern, actionClass, cacheSecs)
  }

  def toRoute: Route = {
    new Route(
      new HttpMethod(httpMethod),
      compiledPattern, Class.forName(actionClass).asInstanceOf[Class[Action]],
      cacheSecs
    )
  }
}
