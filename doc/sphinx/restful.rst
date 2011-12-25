RESTful APIs
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s4.jpg

You can write RESTful APIs for iPhone, Android applications etc. very easily.

::

  import xitrum.Action
  import xitrum.annotation.{GET, GETs}

  @GETs(Array("/", "/articles"))
  class ArticleIndex extends Action {
    override def execute {
      //...
    }
  }

  @GET("/articles/:id")
  class ArticleShow extends Action {
    override def execute {
      //...
    }
  }

The same for POST, PUT, and DELETE.

For HTTP clients that do not support PUT and DELETE (like normal browsers), to
simulate PUT and DELETE, send a POST with _method=put or _method=delete in the
request body.

On web application startup, Xitrum will scan all those annotations, build the
routing table and print it out for you so that you know what APIs your
application has, like this:

::

  [INFO] Routes:
  GET / quickstart.action.IndexAction

You don't need a seperate central config file like routes.rb of Rails.
Annotation is used for URL routes, in the spirit of JAX-RS and Rails Engines.
You don't have to declare all routes in a single place. Think of annotations
as distributed routes.You can plug an app into another app. If you have a
blog engine, you can package it as a JAR file. Then you can plug that JAR file
into another app.

Route cache
-----------

For better startup speed, routes are cached to file ``routes.sclasner``.
While developing, routes in .class files in the ``target`` directory are not
cached. If you change library dependencies that contain routes, you may need to
delete ``routes.sclasner``. This file should not be committed to your project
source code repository.

Route order with @First and @Last
---------------------------------

When you want to route like this:

::

  /articles/:id --> ArticleShow
  /articles/new --> ArticleNew

You must make sure the second route be checked first. ``@First`` is for that purpose:

::

  @GET("/articles/:id")
  class ArticleShow...

  @First
  @GET("/articles/new")
  class ArticleNew...

``@Last`` is similar.

Anti-CSRF
---------

For non-GET requests, Xitrum protects your web application from
`Cross-site request forgery <http://en.wikipedia.org/wiki/CSRF>`_ by default.

When you include ``antiCSRFMeta`` in your layout:

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsAtBottom}
        </body>
      </html>
    )
  }

The ``<head>`` part will include something like this:

::

  <html>
    <head>
      ...
      <meta name="antiCSRFToken" content="5402330e-9916-40d8-a3f4-16b271d583be" />
      ...
    </head>
    ...
  </html>

The token will be automatically included in all non-GET Ajax requests sent by
jQuery.

SkipCSRFCheck
-------------

When you create APIs for machines, e.g. smartphones, you may want to skip this
check. To skip for an action (and its subclasses), make your action extend the
trait xitrum.SkipCSRFCheck:

::

  import xitrum.{Action, SkipCSRFCheck}
  import xitrum.annotation.POST

  trait API extends Action with SkipCSRFCheck

  @POST("/api/positions")
  class LogPositionAPI extends API {
    override def execute {
      //...
    }
  }

  @POST("/api/todos")
  class CreateTodoAPI extends API {
    override def execute {
      //...
    }
  }
