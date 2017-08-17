package xitrum.routing

import java.util.Date

import org.apache.commons.lang3.text.WordUtils
import org.json4s.{JField, JsonAST}
import org.json4s.JsonAST.{JArray, JObject, JString, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods
import xitrum.annotation.Swagger.JsonField
import xitrum.{Config, FutureAction}
import xitrum.annotation.{First, GET, Swagger, SwaggerTypes}

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

@First
@GET("xitrum/swagger.json")
class SwaggerJson extends FutureAction {
	def execute() {
		respondJsonText(SwaggerJson.json)
	}
}

/** Easy-to-remember path to Swagger UI: /xitrum/swagger */
@First
@GET("xitrum/swagger")
class SwaggerUi extends FutureAction {
	def execute() {
		redirectTo(webJarsUrl("swagger-ui/3.0.10/dist/index.html?url=" + url[SwaggerJson]))
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

	def json: String = prettyJson

	private lazy val definitions = mutable.Map.empty[String, JValue]

	/** Maybe called multiple times in development mode when reloading routes. */
	def reloadFromRoutes() {
		prettyJson = JsonMethods.pretty(JsonMethods.render(getSwaggerObject))
	}

	//----------------------------------------------------------------------------

	private def getSwaggerObject: JValue = {
		val info = ("title" -> "APIs documented by Swagger") ~ ("version" -> apiVersion)
		val basePath = if (Config.baseUrl.isEmpty) "/" else Config.baseUrl
		("swagger" -> SWAGGER_VERSION) ~
			("info" -> info) ~
			("basePath" -> basePath) ~
			("paths" -> getPathsObject) ~
			("definitions" -> getDefinitionsObject)
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

	private def getDefinitionsObject: JValue = {
		JObject(definitions.toList: _*)
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
		val args = swagger.swaggerArgs

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
			case (None, None) => None
			case (Some(s), Some(c)) => Some("summary" -> s"$s $c")
			case _ => Some("summary" -> so.orElse(co).get)
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
					("url" -> externalDocs.url)
		}
	}

	private def _className(clazz: Class[_]): String = {
		val pack = clazz.getPackage.getName
		val name = clazz.getTypeName
		name.substring(pack.length + 1)
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
		}.sortBy { arg => // Sort required params first, optional params later
			if (arg.isInstanceOf[SwaggerOptParamArg]) 0 else -1
		}.map { arg =>
			getParameter(arg.asInstanceOf[SwaggerParamArg])
		}
		if (params.isEmpty) None else Some("parameters" -> JArray(params.toList))
	}

	private def _objectField(field: JsonField): (String, JObject) = {
		field.name -> _autoJsonType(field.tpe, asField_? = true)
	}

	private def _addObjectDefinition(tpe: Swagger.JsonType): Unit = {
		if (tpe.tpe == Swagger.JsonType.tpe.obj) {
			val key = tpe.name.get
			if (!definitions.contains(key)) {
				var definition = ("type" -> "object") ~
					("properties" -> JObject(tpe.fields.map(_objectField): _*))
				val required = tpe.fields.filter(_.isRequired_?).map(_.name)
				if (required.nonEmpty)
					definition = definition ~
						("required" -> required)
				definitions.put(key, definition)
			}
		}
	}

	private def _definitionPath(tpe: Swagger.JsonType): String = s"#/definitions/${tpe.name.get}"

	private def _simpleType(tipe: String, format: String, withoutSchema_? : Boolean): JObject = {
		val tpe = ("type" -> tipe) ~
			("format" -> format)
		if (withoutSchema_?)
			tpe
		else "schema" -> tpe
	}

	private def _simpleArrayType(tipe: String, format: String, withoutSchema_? : Boolean): JObject = {
		val tpe = ("type" -> "array") ~
			("items" -> _simpleType(tipe, format, withoutSchema_? = true))
		if (withoutSchema_?)
			tpe
		else "schema" -> tpe
	}

	private def _objectArrayType(path: String, withoutSchema_? : Boolean): JObject = {
		val tpe = ("type" -> "array") ~
			("items" -> ("$ref" -> path))
		if (withoutSchema_?)
			tpe
		else "schema" -> tpe
	}


	private def _objectType(path: String, withoutSchema_? : Boolean): JObject = {
		val tpe = "$ref" -> path
		if (withoutSchema_?)
			tpe
		else "schema" -> tpe
	}

	private def _autoJsonType(tpe: Swagger.JsonType, asField_? : Boolean = false): JObject = {
		_addObjectDefinition(tpe)
		tpe.tpe match {
			case Swagger.JsonType.tpe.obj =>
				if (tpe.isArray_?) {
					_objectArrayType(_definitionPath(tpe), asField_?)
				} else {
					_objectType(_definitionPath(tpe), asField_?)
				}
			case _ =>
				if (tpe.isArray_?) {
					_simpleArrayType(tpe.tpe, tpe.format, asField_?)
				} else {
					_simpleType(tpe.tpe, tpe.format, asField_?)
				}
		}
	}

	private def getParameter(arg: SwaggerParamArg): JValue = {
		val location = arg match {
			case _: SwaggerPathParam => "path"
			case _: SwaggerQueryParam => "query"
			case _: SwaggerHeaderParam => "header"
			case _: SwaggerBodyParam => "body"
			case _ => "formData"
		}

		val required = !arg.isInstanceOf[SwaggerOptParamArg]

		("name" -> arg.name) ~
			("in" -> location) ~
			("description" -> arg.desc) ~
			("required" -> required) ~
			(arg match {
				case _: SwaggerIntParam => _simpleType(Swagger.JsonType.tpe.integer, Swagger.JsonType.fmt.int32, withoutSchema_? = true)
				case _: SwaggerLongParam => _simpleType(Swagger.JsonType.tpe.integer, Swagger.JsonType.fmt.int64, withoutSchema_? = true)
				case _: SwaggerFloatParam => _simpleType(Swagger.JsonType.tpe.number, Swagger.JsonType.fmt.float, withoutSchema_? = true)
				case _: SwaggerDoubleParam => _simpleType(Swagger.JsonType.tpe.number, Swagger.JsonType.fmt.double, withoutSchema_? = true)
				case _: SwaggerStringParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.none, withoutSchema_? = true)
				case _: SwaggerByteParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.byte, withoutSchema_? = true)
				case _: SwaggerBinaryParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.binary, withoutSchema_? = true)
				case _: SwaggerBooleanParam => _simpleType(Swagger.JsonType.tpe.boolean, Swagger.JsonType.fmt.none, withoutSchema_? = true)
				case _: SwaggerDateParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.date, withoutSchema_? = true)
				case _: SwaggerDateTimeParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.dateTime, withoutSchema_? = true)
				case _: SwaggerPasswordParam => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.password, withoutSchema_? = true)
				case _: SwaggerFileParam => _simpleType(Swagger.JsonType.tpe.file, Swagger.JsonType.fmt.none, withoutSchema_? = true)
				case _: SwaggerJsonParam => arg match {
					case argJson: Swagger.JsonBody => _autoJsonType(argJson.tpe)
					case _ => _simpleType(Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.none, withoutSchema_? = true)
				}
			})
	}


	private def getResponses(args: Seq[SwaggerArg]): Option[JField] = {
		val codeToDescs = args.filter(_.isInstanceOf[Swagger.Response]).map { arg =>
			val response = arg.asInstanceOf[Swagger.Response]
			val code = if (response.code <= 0) "default" else response.code.toString
			val desc = response.desc
			var details = JObject("description" -> JString(desc))
			response.tpeOpt match {
				case Some(tpe) =>
					details = details ~ _autoJsonType(tpe)
				case None =>
			}
			code -> details
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
