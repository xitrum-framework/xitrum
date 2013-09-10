package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import xitrum.{Action, Config}
import xitrum.annotation.{First, DELETE, GET, OPTIONS, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.swagger.{Swagger, SwaggerParam, SwaggerResponse}

case class ApiMethod(method: String, route: String)

@First
@GET("/xitrum/swagger.json")
@Swagger(summary = "API doc", notes = "Use this route in Swagger UI to see the doc")
class SwaggerAction extends Action {
  beforeFilter {
    if (Config.productionMode) {
      response.setStatus(HttpResponseStatus.NOT_FOUND)
      respondText(
        "For security reason, Swagger Doc is disabled in production mode. " +
        "If you want to use it in production mode, run in development mode and " +
        "save /xitrum/swagger.json as a static file in public directory."
      )
      false
    } else {
      true
    }
  }

  def execute() {
    val header =
      // Make this an option in xitrum.conf?
      //("apiVersion"     -> "1.0") ~
      ("basePath"       -> absUrlPrefix) ~
      ("swaggerVersion" -> "1.2") ~
      ("resourcePath"   -> url[SwaggerAction])

    val apis = for {
      route <- routes
      doc   <- docOf(route.klass)
      json  <- route2Json(route, doc)
    } yield json

    val json = pretty(render(header ~ ("apis" -> apis)))
    respondJsonText(json)
  }

  private def docOf(klass: Class[_]): Option[Swagger] =
    Option(klass.getAnnotation(classOf[Swagger]))

  private def route2Json(route: Route, doc: Swagger): Option[JObject] = {
    val routePath = RouteCompiler.decompile(route.compiledPattern, true)
    val nickname  = route.klass.getSimpleName

    val params    = doc.params.map(param2json(_))
    val responses = doc.responses.map(response2json(_))

    val cacheNote = cache(route)
    val notes     = if (cacheNote.isEmpty) doc.notes else s"${doc.notes} ${cacheNote}"

    val operations = Seq[JObject](
      ("httpMethod"       -> route.httpMethod.toString) ~
      ("summary"          -> doc.summary) ~
      ("notes"            -> notes) ~
      ("nickname"         -> nickname) ~
      ("parameters"       -> params.toSeq) ~
      ("responseMessages" -> responses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  private def param2json(param: SwaggerParam): JObject = {
    ("name"        -> param.name) ~
    ("paramType"   -> param.paramType) ~
    ("type"        -> param.valueType) ~
    ("description" -> param.description) ~
    ("required"    -> param.required)
  }

  private def response2json(response: SwaggerResponse): JObject = {
    ("code"    -> response.code) ~
    ("message" -> response.message)
  }

  private def annotation2method(annotation: Any): Option[ApiMethod] = annotation match {
    case method: GET       => Some(ApiMethod("GET",       method.value))
    case method: POST      => Some(ApiMethod("POST",      method.value))
    case method: PUT       => Some(ApiMethod("PUT",       method.value))
    case method: DELETE    => Some(ApiMethod("DELETE",    method.value))
    case method: PATCH     => Some(ApiMethod("PATCH",     method.value))
    case method: OPTIONS   => Some(ApiMethod("OPTIONS",   method.value))
    case method: SOCKJS    => Some(ApiMethod("SOCKJS",    method.value))
    case method: WEBSOCKET => Some(ApiMethod("WEBSOCKET", method.value))
    case _                 => None
  }

  private def routes = {
    import Config.routes._
    firstGETs    ++ otherGETs    ++ lastGETs   ++
    firstPOSTs   ++ otherPOSTs   ++ lastPOSTs  ++
    firstPUTs    ++ otherPUTs    ++ lastPUTs   ++
    firstPATCHs  ++ otherPATCHs  ++ lastPATCHs ++
    firstDELETEs ++ otherDELETEs ++ lastDELETEs
  }

  private def cache(route: Route): String = {
    val secs = route.cacheSecs
    if (route.cacheSecs == 0)
      ""
    else if (secs > 0)
      s"(page cache: ${route.cacheSecs} [sec])"
    else
      s"(action cache: ${-route.cacheSecs} [sec])"
  }
}
