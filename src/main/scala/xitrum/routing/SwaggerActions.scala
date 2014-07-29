package xitrum.routing

import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import io.netty.handler.codec.http.HttpResponseStatus

import xitrum.{Action, Config}
import xitrum.annotation.{First, DELETE, GET, PATCH, POST, PUT, SOCKJS, WEBSOCKET}
import xitrum.annotation.{Swagger, SwaggerArg}
import xitrum.view.DocType

case class ApiMethod(method: String, route: String)

object SwaggerJson {
  // See class SwaggerJsonAction.
  // Cache result of SwaggerAction at 1st access;
  // Can't cache header because a server may have multiple addresses
  var apis = loadApis()

  /** Maybe called multiple times in development mode when reloading routes. */
  def loadApis() = for {
    route <- Config.routes.all.flatten
    doc   <- docOf(route.klass)
    json  <- route2Json(route, doc)
  } yield json

  //----------------------------------------------------------------------------

  private def docOf(klass: Class[_ <: Action]): Option[Swagger] = Config.routes.swaggerMap.get(klass)

  private def route2Json(route: Route, doc: Swagger): Option[JObject] = {
    val routePath = RouteCompiler.decompile(route.compiledPattern, true)
    val nickname  = route.klass.getSimpleName

    val summary   = doc.varargs.find(_.isInstanceOf[Swagger.Summary]).asInstanceOf[Option[Swagger.Summary]].map(_.summary).getOrElse("")
    val notes     = doc.varargs.filter(_.isInstanceOf[Swagger.Note]).asInstanceOf[Seq[Swagger.Note]].map(_.note).mkString(" ")
    val responses = doc.varargs.filter(_.isInstanceOf[Swagger.Response]).asInstanceOf[Seq[Swagger.Response]].map(response2json)
    val params    = doc.varargs.filterNot { arg =>
      arg.isInstanceOf[Swagger.Summary] || arg.isInstanceOf[Swagger.Note] || arg.isInstanceOf[Swagger.Response]
    }.sortBy(
      _.toString.indexOf("Opt")  // Required params first, optional params later
    ).map(
      param2json
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
      ("responseMessages" -> responses.toSeq))

    Some(("path" -> routePath) ~ ("operations" -> operations))
  }

  private def param2json(param: SwaggerArg): JObject = {
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

@First
@GET("xitrum/swagger.json")
@Swagger(
  Swagger.Summary("JSON for Swagger Doc of this whole project"),
  Swagger.Note("Use this route in Swagger UI to see API doc.")
)
class SwaggerJson extends Action {
  def execute() {
    // Swagger routes are not collected if swaggerApiVersion is None
    val apiVersion = Config.xitrum.swaggerApiVersion.get

    // relPath may already contain baseUrl, remove it to get resourcePath
    val relPath      = url[SwaggerJson]
    val baseUrl      = Config.baseUrl
    val resourcePath = if (baseUrl.isEmpty) relPath else relPath.substring(baseUrl.length)

    val header =
      ("apiVersion"     -> apiVersion) ~
      ("basePath"       -> absUrlPrefix) ~
      ("swaggerVersion" -> "1.2") ~
      ("resourcePath"   -> resourcePath)

    val json = pretty(render(header ~ ("apis" -> SwaggerJson.apis)))
    respondJsonText(json)
  }
}

/** This path is for users to easily remember: http(s)://host[:port]/xitrum/swagger */
@First
@GET("xitrum/swagger")
class SwaggerUi extends Action {
  def execute() {
    redirectTo[SwaggerUiVersioned]()
  }
}

/** The directory path should be the same with other Swagger UI files. */
@First
@GET("webjars/swagger-ui/2.0.18/index")
class SwaggerUiVersioned extends Action {
  def execute() {
    val swaggerJsonUrl = url[SwaggerJson]

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
    var swaggerJsonUrl = '{swaggerJsonUrl}';

    <xml:unparsed>
    $(function () {
      window.swaggerUi = new SwaggerUi({
        url: swaggerJsonUrl,
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
