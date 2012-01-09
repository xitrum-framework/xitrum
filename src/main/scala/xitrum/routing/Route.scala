package xitrum.routing

import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}
import xitrum.Config

case class Route(httpMethod: HttpMethod, order: RouteOrder.RouteOrder, compiledPattern: CompiledPattern) {
  def url(params: (String, Any)*) = {
    var map = params.toMap
    val tokens = compiledPattern.map { case (token, constant) =>
      if (constant) {
        token
      } else {
        val ret = map(token)
        map = map - token
        ret
      }
    }
    val url = Config.withBaseUrl("/" + tokens.mkString("/"))

    val qse = new QueryStringEncoder(url, Config.requestCharset)
    for ((k, v) <- map) qse.addParam(k, v.toString)
    qse.toString
  }
}
