RESTful APIs
============

You can write RESTful APIs for iPhone, Android applications etc. very easily.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("articles")
  class ArticlesIndex extends Action {
    def execute() {...}
  }
  
  @GET("articles/:id")
  class ArticlesShow extends Action {
    def execute() {...}
  }

The same for POST, PUT, PATCH, DELETE, and OPTIONS.
Xitrum automatically handles HEAD as GET with empty response body.

For HTTP clients that do not support PUT and DELETE (like normal browsers), to
simulate PUT and DELETE, send a POST with _method=put or _method=delete in the
request body.

On web application startup, Xitrum will scan all those annotations, build the
routing table and print it out for you so that you know what APIs your
application has, like this:

::

  [INFO] Routes:
  GET /articles     quickstart.action.ArticlesIndex
  GET /articles/:id quickstart.action.ArticlesShow

Routes are automatically collected in the spirit of JAX-RS
and Rails Engines. You don't have to declare all routes in a single place.
Think of this feature as distributed routes. You can plug an app into another app.
If you have a blog engine, you can package it as a JAR file, then you can put
that JAR file into another app and that app automatically has blog feature!
Routing is also two-way: you can recreate URLs (reverse routing) in a typesafe way.

Route cache
-----------

For better startup speed, routes are cached to file ``routes.cache``.
While developing, routes in .class files in the ``target`` directory are not
cached. If you change library dependencies that contain routes, you may need to
delete ``routes.cache``. This file should not be committed to your project
source code repository.

Route order with first and last
---------------------------------

When you want to route like this:

::

  /articles/:id --> ArticlesShow
  /articles/new --> ArticlesNew

You must make sure the second route be checked first. ``First`` is for this purpose:

::

  import xitrum.annotation.{GET, First}

  @First
  @GET("articles/:id")
  class ArticlesShow extends Action {
    def execute() {...}
  }
  
  @GET("articles/new")
  class ArticlesNew extends Action {
    def execute() {...}
  }

``Last`` is similar.

Multiple paths for one action
-----------------------------

::

  @GET("image", "image/:format")
  class Image extends Action {
    def execute() {
      val format = paramo("format").getOrElse("png")
      // ...
    }
  }

Dot in route
------------

::

  @GET("articles/:id", "articles/:id.:format")
  class ArticlesShow extends Action {
    def execute() {
      val id     = param[Int]("id")
      val format = paramo("format").getOrElse("html")
      // ...
    }
  }

Regex in route
--------------

Regex can be used in routes to specify requirements:

::

  def show = GET("/articles/:id<[0-9]+>") { ... }

Anti-CSRF
---------

For non-GET requests, Xitrum protects your web application from
`Cross-site request forgery <http://en.wikipedia.org/wiki/CSRF>`_ by default.

When you include ``antiCsrfMeta`` in your layout:

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCsrfMeta}
          {xitrumCss}
          {jsDefaults}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )
  }

The ``<head>`` part will include something like this:

::

  <!DOCTYPE html>
  <html>
    <head>
      ...
      <meta name="csrf-token" content="5402330e-9916-40d8-a3f4-16b271d583be" />
      ...
    </head>
    ...
  </html>

The token will be automatically included in all non-GET Ajax requests as
``X-CSRF-Token`` header sent by jQuery if you include
`xitrum.js <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/js.scala>`_
in your view template. xitrum.js is included in ``jsDefaults``.

antiCsrfInput and antiCsrfToken
-------------------------------

Xitrum takes CSRF token from ``X-CSRF-Token`` request header. If the header does
not exists, Xitrum takes the token from ``csrf-token`` request body param
(note: not param in the URL).

If you manually write forms, and you don't use the meta tag and xitrum.js as
described in the previous section, you need to use ``antiCsrfInput`` or
``antiCsrfToken``:

::

  form(method="post" action={url[AdminAddGroup]})
    != antiCsrfInput

::    

  form(method="post" action={url[AdminAddGroup]})
    input(type="hidden" name="csrf-token" value={antiCsrfToken})

SkipCsrfCheck
-------------

When you create APIs for machines, e.g. smartphones, you may want to skip this
automatic CSRF check. Add the trait xitrum.SkipCsrfCheck to you action:

::

  import xitrum.{Action, SkipCsrfCheck}
  import xitrum.annotatin.POST

  trait Api extends Action with SkipCsrfCheck

  @POST("api/positions")
  class LogPositionAPI extends Api {
    def execute() {...}
  }

  @POST("api/todos")
  class CreateTodoAPI extends Api {
    def execute() {...}
  }

Getting entire request body
---------------------------

To get the entire request body, use `request.getContent <http://netty.io/3.6/api/org/jboss/netty/handler/codec/http/HttpRequest.html>`_.
It returns `ChannelBuffer <http://netty.io/3.6/api/org/jboss/netty/buffer/ChannelBuffer.html>`_,
which has ``toString(Charset)`` method.

::

  val body = request.getContent.toString(io.netty.util.CharsetUtil.UTF_8)

Documenting API
---------------

You can document your API with `Swagger <https://developers.helloreverb.com/swagger/>`_
out of the box. Add ``@Swagger`` annotation on actions that need to be documented.
Xitrum will generate `/xitrum/swagger.json <https://github.com/wordnik/swagger-core/wiki/API-Declaration>`_.
This file can be used with `Swagger UI <https://github.com/wordnik/swagger-ui>`_
to generate interactive API documentation.

Xitrum includes Swagger UI. Access it at the path ``/xitrum/swagger`` of your program,
e.g. http://localhost:8000/xitrum/swagger.

.. image:: swagger.png

Let's see `an example <https://github.com/georgeOsdDev/xitrum-placeholder>`_:

::

  import xitrum.{Action, SkipCsrfCheck}
  import xitrum.annotation.{GET, Swagger}

  @Swagger(
    Swagger.Note("Dimensions should not be bigger than 2000 x 2000")
    Swagger.OptStringQuery("text", "Text to render on the image, default: Placeholder"),
    Swagger.Response(200, "PNG image"),
    Swagger.Response(400, "Width or height is invalid or too big")
  )
  trait ImageApi extends Action with SkipCsrfCheck {
    lazy val text = paramo("text").getOrElse("Placeholder")
  }

  @GET("image/:width/:height")
  @Swagger(  // <-- Inherits other info from ImageApi
    Swagger.Summary("Generate rectangle image"),
    Swagger.IntPath("width"),
    Swagger.IntPath("height")
  )
  class RectImageApi extends Api {
    def execute {
      val width  = param[Int]("width")
      val height = param[Int]("height")
      // ...
    }
  }

  @GET("image/:width")
  @Swagger(  // <-- Inherits other info from ImageApi
    Swagger.Summary("Generate square image"),
    Swagger.IntPath("width")
  )
  class SquareImageApi extends Api {
    def execute {
      val width  = param[Int]("width")
      // ...
    }
  }

/xitrum/swagger.json will look like this (note the inheritance):

::

  {
    "basePath":"http://localhost:8000",
    "swaggerVersion":"1.2",
    "resourcePath":"/xitrum/swagger.json",
    "apis":[{
      "path":"/xitrum/swagger.json",
      "operations":[{
        "httpMethod":"GET",
        "summary":"JSON for Swagger Doc of this whole project",
        "notes":"Use this route in Swagger UI to see API doc.",
        "nickname":"SwaggerAction",
        "parameters":[],
        "responseMessages":[]
      }]
    },{
      "path":"/image/{width}/{height}",
      "operations":[{
        "httpMethod":"GET",
        "summary":"Generate rectangle image",
        "notes":"Dimensions should not be bigger than 2000 x 2000",
        "nickname":"RectImageApi",
        "parameters":[{
          "name":"width",
          "paramType":"path",
          "type":"integer",
          "required":true
        },{
          "name":"height",
          "paramType":"path",
          "type":"integer",
          "required":true
        },{
          "name":"text",
          "paramType":"query",
          "type":"string",
          "description":"Text to render on the image, default: Placeholder",
          "required":false
        }],
        "responseMessages":[{
          "code":"200",
          "message":"PNG image"
        },{
          "code":"400",
          "message":"Width is invalid or too big"
        }]
      }]
    },{
      "path":"/image/{width}",
      "operations":[{
        "httpMethod":"GET",
        "summary":"Generate square image",
        "notes":"Dimensions should not be bigger than 2000 x 2000",
        "nickname":"SquareImageApi",
        "parameters":[{
          "name":"width",
          "paramType":"path",
          "type":"integer",
          "required":true
        },{
          "name":"text",
          "paramType":"query",
          "type":"string",
          "description":"Text to render on the image, default: Placeholder",
          "required":false
        }],
        "responseMessages":[{
          "code":"200",
          "message":"PNG image"
        },{
          "code":"400",
          "message":"Width is invalid or too big"
        }]
      }]
    }]
  }

Swagger UI uses the above information to generate interactive API doc.

Params other than Swagger.IntPath and Swagger.OptStringQuery above: BytePath, IntQuery, OptStringForm etc.
They are in the form:

* <Value type><Param type> (required parameter)
* Opt<Value type><Param type> (optional parameter)

Value type: Byte, Int, Int32, Int64, Long, Number, Float, Double, String, Boolean, Date DateTime

Param type: Path, Query, Body, Header, Form

Read more about `value type <https://github.com/wordnik/swagger-core/wiki/Datatypes>`_
and `param type <https://github.com/wordnik/swagger-core/wiki/Parameters>`_.
