package xitrum.routing

import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import xitrum.Action
import xitrum.Config
import xitrum.annotation.DELETE
import xitrum.annotation.GET
import xitrum.annotation.PATCH
import xitrum.annotation.POST
import xitrum.annotation.PUT
import xitrum.swagger.SwaggerDoc
import xitrum.swagger.SwaggerErrorResponse
import xitrum.swagger.SwaggerParameter
import xitrum.swagger.SwaggerParameter
import xitrum.swagger.SwaggerParameter

case class ApiMethod(method: String, route: String)

@GET("/api-docs.json")
@SwaggerDoc(summary = "Swagger api integration", notes = "Use this route in swagger-ui to see the doc")
class SwaggerDocAction extends Action {

  def execute = {
    val json =
      ("apiVersion" -> "1.0") ~
        ("basePath" -> basePath) ~
        ("swaggerVersion" -> "1.2") ~
        ("resourcePath" -> "api")

    val apis = for {
      route <- routes
      doc <- docOf(route.klass)
      json <- route2Json(route, doc)
    } yield json

    respondText(pretty(render(json ~ ("apis" -> apis))), "application/json")
  }

  def docOf(klass: Class[_]): Option[SwaggerDoc] = {
    klass.getAnnotation(classOf[SwaggerDoc]) match {
      case null => None
      case doc => Some(doc)
    }
  }

  def route2Json(route: Route, doc: SwaggerDoc): Option[JObject] = {
    val routePath = RouteCompiler.decompile(route.compiledPattern)
    val nickname = route.klass.getSimpleName

    val parameters = for {
      parameter <- doc.parameters
    } yield parameter2json(parameter)

    val errorResponses = for {
      response <- doc.errorResponses
    } yield error2json(response)

    val operations = Seq[JObject](
      ("httpMethod" -> route.httpMethod.toString) ~
        ("summary" -> doc.summary) ~
        ("notes" -> s" ${doc.notes} ${cache(route)}") ~
        ("nickname" -> nickname) ~
        ("parameters" -> parameters.toSeq) ~
        ("errorResponses" -> errorResponses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  def parameter2json(parameter: SwaggerParameter): JObject = {
    ("name" -> parameter.name) ~
      ("type" -> parameter.typename) ~
      ("dataType" -> parameter.typename) ~
      ("description" -> parameter.description) ~
      ("required" -> parameter.required) ~
      ("allowMultiple" -> parameter.allowMultiple)
  }

  def error2json(response: SwaggerErrorResponse): JObject = {
    ("code" -> response.code) ~
      ("reason" -> response.reason)
  }

  def annotation2method(annotation: Any): Option[ApiMethod] = annotation match {
    case method: GET => Some(ApiMethod("GET", method.value()))
    case method: POST => Some(ApiMethod("POST", method.value()))
    case method: PUT => Some(ApiMethod("PUT", method.value()))
    case method: DELETE => Some(ApiMethod("DELETE", method.value()))
    case method: PATCH => Some(ApiMethod("PATCH", method.value()))
    case _ => None
  }

  def routes = {
    import Config.routes._
    firstGETs ++ otherGETs ++ lastGETs ++
      firstPOSTs ++ otherPOSTs ++ lastPOSTs ++
      firstPUTs ++ otherPUTs ++ lastPUTs ++
      firstPATCHs ++ otherPATCHs ++ lastPATCHs ++
      firstDELETEs ++ otherDELETEs ++ lastDELETEs
  }

  def basePath: String = {
    s"/${Config.baseUrl}"
  }
  
  def cache(route: Route): String = route.cacheSecs match {
    case 0 => ""
    case secs => s"(action cache: ${route.cacheSecs} [sec])"
  }

}