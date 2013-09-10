package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import xitrum.{Action, Config}
import xitrum.annotation.{DELETE, GET, OPTIONS, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.swagger.{SwaggerDoc, SwaggerErrorResponse, SwaggerParameter}

case class ApiMethod(method: String, route: String)

@GET("/api-docs.json")
@SwaggerDoc(summary = "Swagger API integration", notes = "Use this route in Swagger UI to see the doc")
class SwaggerDocAction extends Action {
  def execute() {
    val header =
      ("apiVersion"     -> "1.0") ~
      ("basePath"       -> basePath) ~
      ("swaggerVersion" -> "1.2") ~
      ("resourcePath"   -> url[SwaggerDocAction])

    val apis = for {
      route <- routes
      doc   <- docOf(route.klass)
      json  <- route2Json(route, doc)
    } yield json

    val json = pretty(render(header ~ ("apis" -> apis)))
    respondJsonText(json)
  }

  private def docOf(klass: Class[_]): Option[SwaggerDoc] =
    Option(klass.getAnnotation(classOf[SwaggerDoc]))

  private def route2Json(route: Route, doc: SwaggerDoc): Option[JObject] = {
    val routePath = RouteCompiler.decompile(route.compiledPattern)
    val nickname  = route.klass.getSimpleName

    val parameters = for {
      parameter <- doc.parameters
    } yield parameter2json(parameter)

    val errorResponses = for {
      response <- doc.errorResponses
    } yield error2json(response)

    val operations = Seq[JObject](
      ("httpMethod"     -> route.httpMethod.toString) ~
      ("summary"        -> doc.summary) ~
      ("notes"          -> s" ${doc.notes} ${cache(route)}") ~
      ("nickname"       -> nickname) ~
      ("parameters"     -> parameters.toSeq) ~
      ("errorResponses" -> errorResponses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  private def parameter2json(parameter: SwaggerParameter): JObject = {
    ("name"          -> parameter.name) ~
    ("type"          -> parameter.typename) ~
    ("dataType"      -> parameter.typename) ~
    ("description"   -> parameter.description) ~
    ("required"      -> parameter.required) ~
    ("allowMultiple" -> parameter.allowMultiple)
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

  private def basePath: String = {
    s"/${Config.baseUrl}"
  }

  private def cache(route: Route): String = route.cacheSecs match {
    case 0    => ""
    case secs => s"(action cache: ${route.cacheSecs} [sec])"
  }
}
