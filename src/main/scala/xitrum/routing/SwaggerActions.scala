package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import xitrum.{Action, Config}
import xitrum.annotation.{First, DELETE, GET, OPTIONS, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.swagger.{Swagger, SwaggerParam, SwaggerResponse}
import xitrum.view.DocType

case class ApiMethod(method: String, route: String)

object SwaggerJsonAction {
  // See class SwaggerJsonAction.
  // Cache result of SwaggerAction at 1st access;
  // Can't cache header because a server may have multiple addresses
  lazy val apis = for {
    route <- routes
    doc   <- docOf(route.klass)
    json  <- route2Json(route, doc)
  } yield json

  //----------------------------------------------------------------------------

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

@First
@GET("xitrum/swagger.json")
@Swagger(summary = "JSON for Swagger Doc", notes = "Use this route in Swagger UI to see API doc of this whole project")
class SwaggerJsonAction extends Action {
  beforeFilter {
    if (Config.xitrum.swaggerApiVersion.isEmpty) {
      response.setStatus(HttpResponseStatus.NOT_FOUND)
      respondText("Swagger Doc is disabled")
      false
    } else {
      true
    }
  }

  def execute() {
    // The beforeFilter above ensures that this can't be None
    val apiVersion = Config.xitrum.swaggerApiVersion.get

    // relPath may already contain baseUrl, remove it to get resourcePath
    val relPath      = url[SwaggerJsonAction]
    val baseUrl      = Config.baseUrl
    val resourcePath = if (baseUrl.isEmpty) relPath else relPath.substring(baseUrl.length)

    val header =
      ("apiVersion"     -> apiVersion) ~
      ("basePath"       -> absUrlPrefix) ~
      ("swaggerVersion" -> "1.2") ~
      ("resourcePath"   -> resourcePath)

    val json = pretty(render(header ~ ("apis" -> SwaggerJsonAction.apis)))
    respondJsonText(json)
  }
}

@First
@GET("xitrum/swagger")
class SwaggerIndexAction extends Action {
  def execute() {
    // Redirect to index.html of Swagger Doc. index.html of Swagger Doc is modified
    // so that if there's no "url" param, it will load "/xitrum/swagger.json",
    // otherwise it will load the specified URL
    val json     = url[SwaggerJsonAction]
    val res      = resourceUrl("xitrum/swagger-ui-130915/index.html")
    val location = if (json == "/xitrum/swagger.json") res else res + "&url=" + json
    redirectTo(location)
  }
}
