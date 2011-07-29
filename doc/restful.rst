RESTful APIs
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s4.jpg

You can write RESTful APIs for iPhone, Android applications etc. very easily.

::

  import xitrum.Action
  import xitrum.annotation.{GET, GETs}

  @GETs("/", "/articles")
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

Anti-CSRF
---------

For non-GET requests, Xitrum protects your web application from
`Cross-site request forgery <http://en.wikipedia.org/wiki/CSRF>`_ by default.

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
