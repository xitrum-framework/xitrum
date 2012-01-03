package xitrum.routing

import java.lang.reflect.Method
import io.netty.handler.codec.serialization.ClassResolvers

import xitrum.Controller

object ControllerReflection {
  private val classResolver = ClassResolvers.softCachingResolver(getClass.getClassLoader)

  /** @return controller#route */
  def fullFriendlyActionName(route: Route) = {
    val routeMethod         = route.routeMethod
    val controllerClassName = routeMethod.getDeclaringClass.getName
    val routeName           = routeMethod.getName
    controllerClassName + "#" + routeName
  }

  def getRouteMethod(className: String, methodName: String): Option[Method] = {
    val klass = classResolver.resolve(className)
    if (classOf[Controller].isAssignableFrom(klass))
      Some(klass.getMethod(methodName))
    else
      None
  }

  def newControllerAndRoute(routeMethod: Method): (Controller, Route) = {
    val controllerClass  = routeMethod.getDeclaringClass
    val controller       = controllerClass.newInstance().asInstanceOf[Controller]
    val route            = routeMethod.invoke(controller).asInstanceOf[Route]
    val withRouteMethod = Route(route.httpMethod, route.order, route.compiledPattern, route.body, routeMethod, route.cacheSeconds)
    (controller, withRouteMethod)
  }
}
