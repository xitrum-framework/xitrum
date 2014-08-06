package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import io.netty.handler.codec.http.HttpResponseStatus

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
        ("path" -> ("/" + resource.path)) ~ ("description" -> resource.desc)
    }

    val json =
      ("swaggerVersion" -> SWAGGER_VERSION) ~
      ("apiVersion"     -> apiVersion) ~
      ("apis"           -> apis)
    pretty(render(json))
  }

  def resourceJson(apiVersion: String, resourcePatho: Option[String], basePath: String): Option[String] = {
    val keyo = resourcePatho match {
      case None =>
        Some(None)

      case Some(path) =>
        resources.keys.find { ro => ro.isDefined && ro.get.path == path }
    }

    keyo match {
      case None =>
        None

      case Some(key) =>
        resources.get(key).map { swaggerMap =>
          val json =
            ("swaggerVersion" -> SWAGGER_VERSION) ~
            ("apiVersion"     -> apiVersion) ~
            ("basePath"       -> basePath) ~
            ("apis"           -> loadApis(swaggerMap))
          pretty(render(json))
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
    val nickname  = route.klass.getSimpleName

    val produces  = swaggerArgs.filter(_.isInstanceOf[Swagger.Produces]).asInstanceOf[Seq[Swagger.Produces]].map(_.contentTypes).flatten.distinct
    val consumes  = swaggerArgs.filter(_.isInstanceOf[Swagger.Consumes]).asInstanceOf[Seq[Swagger.Consumes]].map(_.contentTypes).flatten.distinct
    val summary   = swaggerArgs.find(_.isInstanceOf[Swagger.Summary]).asInstanceOf[Option[Swagger.Summary]].map(_.summary).getOrElse("")
    val notes     = swaggerArgs.filter(_.isInstanceOf[Swagger.Note]).asInstanceOf[Seq[Swagger.Note]].map(_.note).mkString(" ")
    val responses = swaggerArgs.filter(_.isInstanceOf[Swagger.Response]).asInstanceOf[Seq[Swagger.Response]].map(response2Json)

    val params = swaggerArgs.filterNot { arg =>
      arg.isInstanceOf[Swagger.Resource] ||
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
      s"(Pction cache: ${-route.cacheSecs} [sec])"
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
@GET("xitrum/swagger/:resourcePath")
class SwaggerResource extends FutureAction {
  def execute() {
    val apiVersion    = Config.xitrum.swaggerApiVersion.get
    val resourcePath  = param("resourcePath")
    val resourcePatho = if (resourcePath == SwaggerJson.NONAME_RESOURCE) None else Some(resourcePath)
    SwaggerJson.resourceJson(apiVersion, resourcePatho, absUrlPrefix) match {
      case Some(json) =>
        respondJsonText(json)
      case None =>
        respond404Page()
    }
  }
}

//------------------------------------------------------------------------------
// Swagger UI

/**
 * Easy-to-remember path to Swagger UI:
 * http(s)://host[:port]/xitrum/swagger
 */
@First
@GET("xitrum/swagger-ui")
class SwaggerUi extends FutureAction {
  def execute() {
    redirectTo[SwaggerUiVersioned]()
  }
}

/**
 * Not-so-easy-to-remember path to Swagger UI. The path is longer because the
 * directory must be at the same level with other Swagger UI files.
 */
@First
@GET("webjars/swagger-ui/2.0.18/index")
class SwaggerUiVersioned extends FutureAction {
  def execute() {
    val swaggerResourcesUrl = url[SwaggerResources]

    // Need to update everytime a new Swagger UI version is released
    val html =
<html>
<head>
  {antiCsrfMeta}
  <title>Swagger UI</title>
  <link href='//fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>
  <link href='css/reset.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='css/reset.css' media='print' rel='stylesheet' type='text/css'/>
  <link href='css/screen.css' media='print' rel='stylesheet' type='text/css'/>
  <script type="text/javascript" src="lib/shred.bundle.js"></script>
  <script src='lib/jquery-1.8.0.min.js' type='text/javascript'></script>
  <script src='lib/jquery.slideto.min.js' type='text/javascript'></script>
  <script src='lib/jquery.wiggle.min.js' type='text/javascript'></script>
  <script src='lib/jquery.ba-bbq.min.js' type='text/javascript'></script>
  <script src='lib/handlebars-1.0.0.js' type='text/javascript'></script>
  <script src='lib/underscore-min.js' type='text/javascript'></script>
  <script src='lib/backbone-min.js' type='text/javascript'></script>
  <script src='lib/swagger.js' type='text/javascript'></script>
  <script src='swagger-ui.js' type='text/javascript'></script>
  <script src='lib/highlight.7.3.pack.js' type='text/javascript'></script>

  <!-- enabling this will enable oauth2 implicit scope support -->
  <script src='lib/swagger-oauth.js' type='text/javascript'></script>

  <script type="text/javascript">
    var swaggerResourcesUrl = '{swaggerResourcesUrl}';

    <xml:unparsed>
    $(function () {
      window.swaggerUi = new SwaggerUi({
        url: swaggerResourcesUrl,
        dom_id: "swagger-ui-container",
        supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
        onComplete: function(swaggerApi, swaggerUi){
          log("Loaded SwaggerUI");

          if(typeof initOAuth == "function") {
            /*
            initOAuth({
              clientId: "your-client-id",
              realm: "your-realms",
              appName: "your-app-name"
            });
            */
          }
          $('pre code').each(function(i, e) {
            hljs.highlightBlock(e)
          });
        },
        onFailure: function(data) {
          log("Unable to Load SwaggerUI");
        },
        docExpansion: "none"
      });

      $('#input_apiKey').change(function() {
        var key = $('#input_apiKey')[0].value;
        log("key: " + key);
        if(key && key.trim() != "") {
          log("added key " + key);
          window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
        }
      })
      window.swaggerUi.load();

      // Set CSRF token for all Ajax requests
      // https://github.com/wordnik/swagger-ui#custom-header-parameters---for-basic-auth-etc
      var token = $("meta[name='csrf-token']").attr('content');
      window.authorizations.add('X-CSRF-Token', new ApiKeyAuthorization('X-CSRF-Token', token, "header"));
    });
    </xml:unparsed>
  </script>
</head>

<body class="swagger-section">
<div id='header'>
  <div class="swagger-ui-wrap">
    <a id="logo" href="http://swagger.wordnik.com">swagger</a>
    <form id='api_selector'>
      <div class='input icon-btn'>
        <img id="show-pet-store-icon" src="images/pet_store_api.png" title="Show Swagger Petstore Example Apis" />
      </div>
      <div class='input icon-btn'>
        <img id="show-wordnik-dev-icon" src="images/wordnik_api.png" title="Show Wordnik Developer Apis" />
      </div>
      <div class='input'><input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl" type="text"/></div>
      <div class='input'><input placeholder="api_key" id="input_apiKey" name="apiKey" type="text"/></div>
      <div class='input'><a id="explore" href="#">Explore</a></div>
    </form>
  </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">&nbsp;</div>
<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
</body>
</html>

    respondHtml(DocType.html5(html))
  }
}
