package xitrum.routing

import org.apache.commons.lang3.text.WordUtils

import org.json4s.JField
import org.json4s.JsonAST.{JString, JArray, JValue, JObject}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import xitrum.{FutureAction, Config}
import xitrum.annotation.{First, GET, Swagger, SwaggerTypes}

import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer

@First
@GET("xitrum/swagger.json")
class SwaggerJson extends FutureAction {
  def execute() {
    respondJsonText(SwaggerJson.json)
  }
}

/** Easy-to-remember path to Swagger UI: /xitrum/swagger-ui */
@First
@GET("xitrum/swagger-ui")
class SwaggerUi extends FutureAction {
  def execute() {
    redirectTo(webJarsUrl("swagger-ui/2.1.4/index.html?url=" + url[SwaggerJson]))
  }
}

/** https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md */
object SwaggerJson {
  import SwaggerTypes._

  private val SWAGGER_VERSION = "2.0"

  private val apiVersion = Config.xitrum.swaggerApiVersion.getOrElse("1.0")

  /** Maybe set multiple times in development mode when reloading routes. */
  private var prettyJson = JsonMethods.pretty(JsonMethods.render(getSwaggerObject))

  //----------------------------------------------------------------------------

  def json = prettyJson

  /** Maybe called multiple times in development mode when reloading routes. */
  def reloadFromRoutes() {
    prettyJson = JsonMethods.pretty(JsonMethods.render(getSwaggerObject))
  }

  //----------------------------------------------------------------------------

  private def getSwaggerObject: JValue = {
    val info     = ("title" -> "APIs documented by Swagger") ~ ("version" -> apiVersion)
    val basePath = if (Config.baseUrl.isEmpty) "/" else Config.baseUrl
    ("swagger"  -> SWAGGER_VERSION) ~
    ("info"     -> info) ~
    ("basePath" -> basePath) ~
    ("paths"    -> getPathsObject)
  }

  private def getPathsObject: JValue = {
    // Shorter paths are often less complex than longer paths.
    // We sort so that the API doc becomes easier to understand.
    val sortedByPath = groupSwaggerRoutesByPath.toSeq.sortBy(_._1.length)

    val pathToPathItemObjects: Seq[(String, JValue)] =
      sortedByPath.map { case (path, routes) =>
        val pathItem = getPathItem(routes)
        (path, pathItem)
      }
    JObject(pathToPathItemObjects: _*)
  }

  private def groupSwaggerRoutesByPath: Map[String, Seq[Route]] = {
    val swaggerMap = Config.routes.swaggerMap

    // Use ListMap to preserve the order of routes
    Config.routes.allFlatten().foldLeft(ListMap.empty[String, ArrayBuffer[Route]]) { (acc, route) =>
      if (swaggerMap.contains(route.klass)) {
        val path = RouteCompiler.decompile(route.compiledPattern, forSwagger = true)
        acc.get(path) match {
          case None =>
            val routes = ArrayBuffer[Route](route)
            acc.updated(path, routes)

          case Some(existingRoutes) =>
            existingRoutes.append(route)
            acc
        }
      } else {
        acc
      }
    }
  }

  private def getPathItem(routes: Seq[Route]): JValue = {
    val methodToOperationObject: Seq[(String, JValue)] = routes.map { route =>
      (route.httpMethod.name.toLowerCase, getOperation(route))
    }
    JObject(methodToOperationObject: _*)
  }

  private def getOperation(route: Route): JValue = {
    val swagger = Config.routes.swaggerMap(route.klass)
    val args    = swagger.swaggerArgs

    val fields = Seq(
      getTags(args),
      getSummary(args, route),
      getDescription(args),
      getExternalDocs(args),
      getOperationId(args, route),
      getConsumes(args),
      getProduces(args),
      getParameters(args),
      getResponses(args),
      getSchemes(args),
      getDeprecated(args)
    ).flatten
    JObject(fields: _*)
  }

  private def getTags(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.Tags]).map { arg =>
      "tags" -> arg.asInstanceOf[Swagger.Tags].tags
    }
  }

  private def getSummary(args: Seq[SwaggerArg], route: Route): Option[JField] = {
    val so = args.find(_.isInstanceOf[Swagger.Summary]).map { arg =>
      arg.asInstanceOf[Swagger.Summary].summary
    }
    val co = getCacheInfo(route)

    (so, co) match {
      case (None,    None)    => None
      case (Some(s), Some(c)) => Some("summary" -> s"$s $c")
      case _                  => Some("summary" -> so.orElse(co).get)
    }
  }

  private def getCacheInfo(route: Route): Option[String] = {
    val secs = route.cacheSecs
    if (route.cacheSecs == 0)
      None
    else if (secs > 0)
      Some(s"(Page cache: ${route.cacheSecs} [sec])")
    else
      Some(s"(Action cache: ${-route.cacheSecs} [sec])")
  }

  private def getDescription(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.Description]).map { arg =>
      "description" -> arg.asInstanceOf[Swagger.Description].desc
    }
  }

  private def getExternalDocs(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.ExternalDocs]).map { arg =>
      val externalDocs = arg.asInstanceOf[Swagger.ExternalDocs]
      "externalDocs" ->
        ("description" -> externalDocs.desc) ~
        ("url"         -> externalDocs.url)
    }
  }

  private def getOperationId(args: Seq[SwaggerArg], route: Route): Option[JField] = {
    val operationId = args.find(_.isInstanceOf[Swagger.OperationId]).map { arg =>
      arg.asInstanceOf[Swagger.OperationId].id
    }.getOrElse(
      WordUtils.uncapitalize(route.klass.getSimpleName)
    )
    Some("operationId" -> operationId)
  }

  private def getConsumes(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.Consumes]).map { arg =>
      "consumes" -> arg.asInstanceOf[Swagger.Consumes].contentTypes
    }
  }

  private def getProduces(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.Produces]).map { arg =>
      "produces" -> arg.asInstanceOf[Swagger.Produces].contentTypes
    }
  }

  private def getParameters(args: Seq[SwaggerArg]): Option[JField] = {
    val params = args.filter { arg =>
      arg.isInstanceOf[SwaggerParamArg]
    }.sortBy { arg =>  // Sort required params first, optional params later
      if (arg.isInstanceOf[SwaggerOptParamArg]) 0 else -1
    }.map { arg =>
      getParameter(arg.asInstanceOf[SwaggerParamArg])
    }
    if (params.isEmpty) None else Some("parameters" -> JArray(params.toList))
  }

  private def getParameter(arg: SwaggerParamArg): JValue = {
    val location = arg match {
      case _: SwaggerPathParam   => "path"
      case _: SwaggerQueryParam  => "query"
      case _: SwaggerHeaderParam => "header"
      case _: SwaggerBodyParam   => "body"
      case _                     => "formData"
    }

    val required = !arg.isInstanceOf[SwaggerOptParamArg]

    val (tipe, format) = arg match {
      case _: SwaggerIntParam      => ("integer",  "int32")
      case _: SwaggerLongParam     => ("integer",  "int64")
      case _: SwaggerFloatParam    => ("number",  "float")
      case _: SwaggerDoubleParam   => ("number",  "double")
      case _: SwaggerStringParam   => ("string",  "")
      case _: SwaggerByteParam     => ("string",  "byte")
      case _: SwaggerBinaryParam   => ("string",  "binary")
      case _: SwaggerBooleanParam  => ("boolean", "")
      case _: SwaggerDateParam     => ("string",  "date")
      case _: SwaggerDateTimeParam => ("string",  "date-time")
      case _: SwaggerPasswordParam => ("string",  "password")
      case _: SwaggerFileParam     => ("file",    "")
    }

    ("name"        -> arg.name) ~
    ("in"          -> location) ~
    ("description" -> arg.desc) ~
    ("required"    -> required) ~
    ("type"        -> tipe) ~
    ("format"      -> format)
  }

  private def getResponses(args: Seq[SwaggerArg]): Option[JField] = {
    val codeToDescs = args.filter(_.isInstanceOf[Swagger.Response]).map { arg =>
      val response = arg.asInstanceOf[Swagger.Response]
      val code = if (response.code <= 0) "default" else response.code.toString
      val desc = response.desc
      code -> JString(desc)
    }
    Some("responses" -> JObject(codeToDescs: _*))
  }

  private def getSchemes(args: Seq[SwaggerArg]): Option[JField] = {
    args.find(_.isInstanceOf[Swagger.Schemes]).map { arg =>
      "schemes" -> arg.asInstanceOf[Swagger.Schemes].schemes
    }
  }

  private def getDeprecated(args: Seq[SwaggerArg]): Option[JField] = {
    if (args.contains(Swagger.Deprecated)) Some("deprecated" -> true) else None
  }
}
