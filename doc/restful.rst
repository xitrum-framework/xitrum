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
routing table and print it out for you (so that you know what APIs your
application has). You don't need a seperate config file like routes.rb of Rails.

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
