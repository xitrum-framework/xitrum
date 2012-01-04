RESTful APIs
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s4.jpg

You can write RESTful APIs for iPhone, Android applications etc. very easily.

::

  import xitrum.Controller

  class Articles extends Controller {
    pathPrefix = "articles"

    val index = GET {...}
    val show  = GET(":id) {...}
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
  GET /articles     quickstart.controller.Articles#index
  GET /articles/:id quickstart.controller.Articles#show

Routes are automatically collected in the spirit of JAX-RS (but without annotations!)
and Rails Engines. You don't have to declare all routes in a single place.
Think of this feature as distributed routes. You can plug an app into another app.
If you have a blog engine, you can package it as a JAR file, then you can put
that JAR file into another app and that app automatically has blog feature!
Routing is also two-way: you can recreate URLs (reverse routing) in a typesafe way.

Route cache
-----------

For better startup speed, routes are cached to file ``routes.sclasner``.
While developing, routes in .class files in the ``target`` directory are not
cached. If you change library dependencies that contain routes, you may need to
delete ``routes.sclasner``. This file should not be committed to your project
source code repository.

Route order with first and last
---------------------------------

When you want to route like this:

::

  /articles/:id --> Articles#show
  /articles/new --> Articles#niw

You must make sure the second route be checked first. ``first`` is for this purpose:

::

  class Articles extends Controller {
    pathPrefix = "articles"

    val show = GET(":id") {...}
    val niw  = first.GET("new") {...}
  }

``last`` is similar.

Anti-CSRF
---------

For non-GET requests, Xitrum protects your web application from
`Cross-site request forgery <http://en.wikipedia.org/wiki/CSRF>`_ by default.

When you include ``antiCSRFMeta`` in your layout:

::

  import xitrum.Controller
  import xitrum.view.DocType

  trait AppController extends Controller {
    override def layout = DocType.html5(
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

  <!DOCTYPE html>
  <html>
    <head>
      ...
      <meta name="csrf-token" content="5402330e-9916-40d8-a3f4-16b271d583be" />
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

  import xitrum.{Controller, SkipCSRFCheck}

  trait API extends Controller with SkipCSRFCheck

  class LogPositionAPI extends API {
    pathPrefix = "api/positions"
    val log = POST {...}
  }

  class CreateTodoAPI extends API {
    pathPrefix = "api/todos"
    val create = POST {...}
  }
