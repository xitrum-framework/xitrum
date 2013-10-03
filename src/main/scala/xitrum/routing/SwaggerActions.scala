package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native._
import org.json4s.native.JsonMethods._

import org.jboss.netty.handler.codec.http.HttpResponseStatus

import xitrum.{Action, Config}
import xitrum.annotation.{First, DELETE, GET, OPTIONS, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.{Swagger, SwaggerParamOrResponse}
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

  private def docOf(klass: Class[_ <: Action]): Option[Swagger] = Config.routes.swaggerMap.get(klass)

  private def route2Json(route: Route, doc: Swagger): Option[JObject] = {
    val routePath = RouteCompiler.decompile(route.compiledPattern, true)
    val nickname  = route.klass.getSimpleName

    val params    = doc.varargs.filterNot(_.isInstanceOf[Swagger.Response]).map(param2json(_))
    val responses = doc.varargs.filter(_.isInstanceOf[Swagger.Response]).asInstanceOf[Seq[Swagger.Response]].map(response2json(_))

    val cacheNote = cache(route)
    val notes     = if (cacheNote.isEmpty) "" else cacheNote

    val operations = Seq[JObject](
      ("httpMethod"       -> route.httpMethod.toString) ~
      ("summary"          -> doc.desc) ~
      ("notes"            -> notes) ~
      ("nickname"         -> nickname) ~
      ("parameters"       -> params.toSeq) ~
      ("responseMessages" -> responses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  private def param2json(param: SwaggerParamOrResponse): JObject = {
    // Use class name to extract paramType, valueType, and required
    // See Swagger.scala

    val klass          = param.getClass
    val className      = klass.getName            // Ex: xitrum.annotation.Swagger$OptionalBytePath
    val shortClassName = className.split('$')(1)  // Ex: OptionalBytePath

    val paramType =
           if (shortClassName.endsWith("Path"))   "path"
      else if (shortClassName.endsWith("Query"))  "query"
      else if (shortClassName.endsWith("Body"))   "body"
      else if (shortClassName.endsWith("Header")) "header"
      else                              "form"

    val required = !shortClassName.startsWith("Optional")

    val valueType =
      if (required)
        shortClassName.substring(0, shortClassName.length - paramType.length).toLowerCase
      else
        shortClassName.substring("Optional".length, shortClassName.length - paramType.length).toLowerCase

    // Use reflection to extract name and desc

    val nameMethod = klass.getMethod("name")
    val name       = nameMethod.invoke(param).asInstanceOf[String]


    val descMethod = klass.getMethod("desc")
    val desc       = descMethod.invoke(param).asInstanceOf[String]

    ("name"        -> name) ~
    ("paramType"   -> paramType) ~
    ("type"        -> valueType) ~
    ("description" -> desc) ~
    ("required"    -> required)
  }

  private def response2json(response: Swagger.Response): JObject = {
    ("code"    -> response.code) ~
    ("message" -> response.desc)
  }

  private def annotation2method(annotation: Any): Seq[ApiMethod] = annotation match {
    case method: GET       => method.paths.map(ApiMethod("GET",       _))
    case method: POST      => method.paths.map(ApiMethod("POST",      _))
    case method: PUT       => method.paths.map(ApiMethod("PUT",       _))
    case method: DELETE    => method.paths.map(ApiMethod("DELETE",    _))
    case method: PATCH     => method.paths.map(ApiMethod("PATCH",     _))
    case method: OPTIONS   => method.paths.map(ApiMethod("OPTIONS",   _))
    case method: SOCKJS    => method.paths.map(ApiMethod("SOCKJS",    _))
    case method: WEBSOCKET => method.paths.map(ApiMethod("WEBSOCKET", _))
    case _                 => Seq()
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
@Swagger("JSON for Swagger Doc. Use this route in Swagger UI to see API doc of this whole project.")
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
    val res      = resourceUrl("xitrum/swagger-ui-2.0.2/index.html")
    val location = if (json == "/xitrum/swagger.json") res else res + "&url=" + json
    redirectTo(location)
  }
}
