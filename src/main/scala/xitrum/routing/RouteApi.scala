package xitrum.routing

/** APIs for app developer to manipulate route config programatically */
trait RouteApi {
  import Routes._

/*
  def append(httpMethod: HttpMethod, pattern: String, routeMethod: Method) {
    val route = (httpMethod, pattern, routeMethod)
    compiledRoutes.append(compileRoute(route))
  }

  def append(httpMethod: String, pattern: String, routeMethod: Method) {
    append(new HttpMethod(httpMethod), pattern, routeMethod)
  }

  def prepend(httpMethod: HttpMethod, pattern: String, routeMethod: Method) {
    val route = (httpMethod, pattern, routeMethod)
    compiledRoutes.prepend(compileRoute(route))
  }

  def prepend(httpMethod: String, pattern: String, routeMethod: Method) {
    prepend(new HttpMethod(httpMethod), pattern, routeMethod)
  }

  def GET(pattern: String, routeMethod: Method, isAppend: Boolean = true) {
    if (isAppend)
      append(HttpMethod.GET, pattern, routeMethod)
    else
      prepend(HttpMethod.GET, pattern, routeMethod)
  }

  def POST(pattern: String, routeMethod: Method, isAppend: Boolean = true) {
    if (isAppend)
      append(HttpMethod.POST, pattern, routeMethod)
    else
      prepend(HttpMethod.POST, pattern, routeMethod)
  }

  def PUT(pattern: String, routeMethod: Method, isAppend: Boolean = true) {
    if (isAppend)
      append(HttpMethod.PUT, pattern, routeMethod)
    else
      prepend(HttpMethod.PUT, pattern, routeMethod)
  }

  def DELETE(pattern: String, routeMethod: Method, isAppend: Boolean = true) {
    if (isAppend)
      append(HttpMethod.DELETE, pattern, routeMethod)
    else
      prepend(HttpMethod.DELETE, pattern, routeMethod)
  }

  def WEBSOCKET(pattern: String, routeMethod: Method, isAppend: Boolean = true) {
    if (isAppend)
      append(WebSocketHttpMethod, pattern, routeMethod)
    else
      prepend(WebSocketHttpMethod, pattern, routeMethod)
  }
*/
}
