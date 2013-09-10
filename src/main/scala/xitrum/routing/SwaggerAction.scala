package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import xitrum.{Action, Config}
import xitrum.annotation.{First, DELETE, GET, OPTIONS, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.swagger.{Swagger, SwaggerErrorResponse, SwaggerParam}

case class ApiMethod(method: String, route: String)

@First
@GET("/swagger.json")
@Swagger(summary = "API doc", notes = "Use this route in Swagger UI to see the doc")
class SwaggerAction extends Action {
  def execute() {
    val header =
      ("apiVersion"     -> "1.0") ~
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

    val params = for {
      param <- doc.params
    } yield param2json(param)

    val errorResponses = for {
      response <- doc.errorResponses
    } yield error2json(response)

    val cacheNote = cache(route)
    val notes     = if (cacheNote.isEmpty) doc.notes else s"${doc.notes} ${cacheNote}"

    val operations = Seq[JObject](
      ("httpMethod"     -> route.httpMethod.toString) ~
      ("summary"        -> doc.summary) ~
      ("notes"          -> notes) ~
      ("nickname"       -> nickname) ~
      ("parameters"     -> params.toSeq) ~
      ("errorResponses" -> errorResponses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  private def param2json(param: SwaggerParam): JObject = {
    ("name"          -> param.name) ~
    ("paramType"     -> param.paramType) ~
    ("type"          -> param.tpe) ~
    ("description"   -> param.description) ~
    ("required"      -> param.required)
  }

  private def error2json(response: SwaggerErrorResponse): JObject = {
    ("code"   -> response.code) ~
    ("reason" -> response.reason)
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
