package xitrum.routing

import org.apache.commons.lang3.text.WordUtils

import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import xitrum.{Action, FutureAction, Config}
import xitrum.annotation.{First, DELETE, GET, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.{Swagger, SwaggerArg}
import xitrum.view.DocType

case class ApiMethod(method: String, route: String)

/** https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md */
object SwaggerJson {
  val SWAGGER_VERSION = "1.2"
  val NONAME_RESOURCE = "noname"

  /** Maybe called multiple times in development mode when reloading routes. */
  var resources = groupByResource()

  def groupByResource(): Map[Option[Swagger.Resource], Map[Class[_ <: Action], Swagger]] = {
    Config.routes.swaggerMap.groupBy { case (klass, swagger) =>
      val swaggerArgs = swagger.swaggerArgs
      swaggerArgs.find(_.isInstanceOf[Swagger.Resource]).asInstanceOf[Option[Swagger.Resource]]
    }
  }

  //----------------------------------------------------------------------------

  def resourcesJson(apiVersion: String): String = {
    val apis = SwaggerJson.resources.keys.map {
      case None =>
        ("path" -> ("/" + NONAME_RESOURCE)) ~ ("description" -> "APIs without named resource")

      case Some(resource) =>
        val path = if (resource.path.startsWith("/")) resource.path else "/" + resource.path
        ("path" -> path) ~ ("description" -> resource.desc)
    }

    val json =
      ("swaggerVersion" -> SWAGGER_VERSION) ~
      ("apiVersion"     -> apiVersion) ~
      ("apis"           -> apis)
    JsonMethods.pretty(JsonMethods.render(json))
  }

  def resourceJson(apiVersion: String, basePath: String, resourcePatho: Option[String]): Option[String] = {
    val keyo = resourcePatho match {
      case None =>
        Some(None)

      case Some(path) =>
        resources.keys.find { ro => ro.isDefined && (ro.get.path == path || ro.get.path == "/" + path) }
    }

    keyo match {
      case None =>
        None

      case Some(key) =>
        resources.get(key).map { swaggerMap =>
          val resourcePath = resourcePatho.getOrElse(NONAME_RESOURCE)
          val path         = if (resourcePath.startsWith("/")) resourcePath else "/" + resourcePath
          val json         =
            ("swaggerVersion" -> SWAGGER_VERSION) ~
            ("apiVersion"     -> apiVersion) ~
            ("basePath"       -> basePath) ~
            ("resourcePath"   -> path) ~
            ("apis"           -> loadApis(swaggerMap))
          JsonMethods.pretty(JsonMethods.render(json))
        }
    }
  }

  //----------------------------------------------------------------------------

  private def loadApis(swaggerMap: Map[Class[_ <: Action], Swagger]): Seq[JObject] = for {
    route   <- Config.routes.all.flatten
    swagger <- swaggerMap.get(route.klass)
  } yield swagger2Json(route, swagger)

  private def swagger2Json(route: Route, swagger: Swagger): JObject = {
    val swaggerArgs = swagger.swaggerArgs

    val routePath = RouteCompiler.decompile(route.compiledPattern, true)

    val nickname  = swaggerArgs.find(_.isInstanceOf[Swagger.Nickname]).map(_.asInstanceOf[Swagger.Nickname].nickname).getOrElse(WordUtils.uncapitalize(route.klass.getSimpleName))
    val produces  = swaggerArgs.filter(_.isInstanceOf[Swagger.Produces]).asInstanceOf[Seq[Swagger.Produces]].flatMap(_.contentTypes).distinct
    val consumes  = swaggerArgs.filter(_.isInstanceOf[Swagger.Consumes]).asInstanceOf[Seq[Swagger.Consumes]].flatMap(_.contentTypes).distinct
    val summary   = swaggerArgs.find(_.isInstanceOf[Swagger.Summary]).asInstanceOf[Option[Swagger.Summary]].map(_.summary).getOrElse("")
    val notes     = swaggerArgs.filter(_.isInstanceOf[Swagger.Note]).asInstanceOf[Seq[Swagger.Note]].map(_.note).mkString(" ")
    val responses = swaggerArgs.filter(_.isInstanceOf[Swagger.Response]).asInstanceOf[Seq[Swagger.Response]].map(response2Json)

    val params = swaggerArgs.filterNot { arg =>
      arg.isInstanceOf[Swagger.Resource] ||
      arg.isInstanceOf[Swagger.Nickname] ||
      arg.isInstanceOf[Swagger.Produces] ||
      arg.isInstanceOf[Swagger.Consumes] ||
      arg.isInstanceOf[Swagger.Summary]  ||
      arg.isInstanceOf[Swagger.Note]     ||
      arg.isInstanceOf[Swagger.Response]
    }.sortBy(
      _.toString.indexOf("Opt")  // Required params first, optional params later
    ).map(
      param2Json
    )

    val cacheNote  = cache(route)
    val finalNotes =
      if (cacheNote.isEmpty)
        notes
      else if (notes.isEmpty)
        cacheNote
      else
        notes + " " + cacheNote

    val operations = Seq[JObject](
      ("httpMethod"       -> route.httpMethod.toString) ~
      ("summary"          -> summary) ~
      ("notes"            -> finalNotes) ~
      ("nickname"         -> nickname) ~
      ("type"             -> "string") ~  // FIXME: Support models
      ("parameters"       -> params.toSeq) ~
      ("responseMessages" -> responses.toSeq) ~
      ("produces"         -> produces) ~
      ("consumes"         -> consumes))

    ("path" -> routePath) ~ ("operations" -> operations)
  }

  private def param2Json(param: SwaggerArg): JObject = {
    // Use class name to extract paramType, valueType, and required
    // See Swagger.scala

    val klass          = param.getClass
    val className      = klass.getName            // Ex: xitrum.annotation.Swagger$OptBytePath
    val shortClassName = className.split('$')(1)  // Ex: OptBytePath

    val paramType =
           if (shortClassName.endsWith("Path"))   "path"
      else if (shortClassName.endsWith("Query"))  "query"
      else if (shortClassName.endsWith("Body"))   "body"
      else if (shortClassName.endsWith("Header")) "header"
      else                                        "form"

    val required = !shortClassName.startsWith("Opt")

    val valueType =
      if (required)
        shortClassName.substring(0, shortClassName.length - paramType.length).toLowerCase
      else
        shortClassName.substring("Opt".length, shortClassName.length - paramType.length).toLowerCase

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

  private def response2Json(response: Swagger.Response): JObject = {
    ("code"    -> response.code) ~
    ("message" -> response.desc)
  }

  private def annotation2Method(annotation: Any): Seq[ApiMethod] = annotation match {
    case method: GET       => method.paths.map(ApiMethod("GET",       _))
    case method: POST      => method.paths.map(ApiMethod("POST",      _))
    case method: PUT       => method.paths.map(ApiMethod("PUT",       _))
    case method: PATCH     => method.paths.map(ApiMethod("PATCH",     _))
    case method: DELETE    => method.paths.map(ApiMethod("DELETE",    _))
    case method: SOCKJS    => method.paths.map(ApiMethod("SOCKJS",    _))
    case method: WEBSOCKET => method.paths.map(ApiMethod("WEBSOCKET", _))
    case _                 => Seq()
  }

  private def cache(route: Route): String = {
    val secs = route.cacheSecs
    if (route.cacheSecs == 0)
      ""
    else if (secs > 0)
      s"(Page cache: ${route.cacheSecs} [sec])"
    else
      s"(Action cache: ${-route.cacheSecs} [sec])"
  }
}

//------------------------------------------------------------------------------

/** Swagger resource listing: https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md */
@First
@GET("xitrum/swagger")
class SwaggerResources extends FutureAction {
  def execute() {
    val apiVersion = Config.xitrum.swaggerApiVersion.get
    val json       = SwaggerJson.resourcesJson(apiVersion)
    respondJsonText(json)
  }
}

/** Swagger API declaration: https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md */
@First
@GET("xitrum/swagger/:*")
class SwaggerResource extends FutureAction {
  def execute() {
    val apiVersion    = Config.xitrum.swaggerApiVersion.get
    val resourcePath  = param("*")
    val resourcePatho = if (resourcePath == SwaggerJson.NONAME_RESOURCE) None else Some(resourcePath)
    SwaggerJson.resourceJson(apiVersion, absUrlPrefix, resourcePatho) match {
      case Some(json) =>
        respondJsonText(json)
      case None =>
        respond404Page()
    }
  }
}

/** Easy-to-remember path to Swagger UI: http(s)://host[:port]/xitrum/swagger-ui */
@First
@GET("xitrum/swagger-ui")
class SwaggerUi extends FutureAction {
  def execute() {
    redirectTo(webJarsUrl("swagger-ui/2.1.1/index.html?url=" + url[SwaggerResources]))
  }
}
