package xitrum.routing

import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}
import xitrum.Config

case class Route(httpMethod: HttpMethod, order: RouteOrder.RouteOrder, compiledPattern: Seq[RouteToken]) {
  def url(params: (String, Any)*) = {
    var map = params.toMap
    val tokens = compiledPattern.map { rt =>
      if (rt.isPlaceHolder) {
        val ret = map(rt.value)
        map = map - rt.value
        ret
      } else {
        rt.value
      }
    }
    val url = Config.withBaseUrl("/" + tokens.mkString("/"))

    val qse = new QueryStringEncoder(url, Config.requestCharset)
    for ((k, v) <- map) qse.addParam(k, v.toString)
    qse.toString
  }
}
