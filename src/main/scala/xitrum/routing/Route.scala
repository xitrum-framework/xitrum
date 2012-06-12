package xitrum.routing

import org.jboss.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}
import xitrum.Config

case class Route(httpMethod: HttpMethod, order: RouteOrder.RouteOrder, compiledPattern: Seq[RouteToken]) {
  def url(params: (String, Any)*) = {
    var map = params.toMap
    val tokens = compiledPattern.map { rt =>
      if (rt.isPlaceHolder) {
        val key = rt.value
        if (!map.isDefinedAt(key))
          throw new Exception("Cannot compute reverse URL because there's no required key \"" + key + "\"")

        val ret = map(key)
        map = map - key
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
