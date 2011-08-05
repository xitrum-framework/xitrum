Scopes
======

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s11.jpg

Request
-------

Kinds of params
~~~~~~~~~~~~~~~

There are 2 kinds of request params: textual params and file upload params (binary).

There are 3 kinds of textual params, of type ``scala.collection.mutable.Map[String, List[String]]``:

1. ``uriParams``: params after the ? mark in the URL, example: http://example.com/blah?x=1&y=2
2. ``bodyParams``: params in POST request body
3. ``pathParams``: params embedded in the URL, example: ``@GET("/articles/:id/:title")``

These params are merged in the above order (from 1 to 3, the latter will
override the former), as ``textParams``.

``fileUploadParams`` is of type ``scala.collection.mutable.Map[String, List[org.jboss.netty.handler.codec.http.FileUpload]]``.

Accesing params
~~~~~~~~~~~~~~~

From an action, you can access the above params directly, or you can use
accessor methods.

To access ``textParams``:

* ``param("x")``: returns ``String``, throws exception if x does not exist
* ``params("x")``: returns ``List[String]``, throws exception if x does not exist
* ``paramo("x")``: returns ``Option[String]``
* ``paramso("x")``: returns ``Option[List[String]]``

You can convert String to other types (Int, Long, Fload, Double) automatically:

* ``tparam[Int]("x")``: returns ``Int``, throws exception if x does not exist
* ``tparams[Int]("x")``: returns ``List[Int]``, throws exception if x does not exist
* ``tparamo[Int]("x")``: returns ``Option[Int]``
* ``tparamso[Int]("x")``: returns ``Option[List[Int]]``

For file upload:

* ``uploadParam("x")``: returns ``FileUpload``, throws exception if x does not exist
* ``uploadParams("x")``: returns ``List[FileUpload]``, throws exception if x does not exist
* ``uploadParamo("x")``: returns ``Option[FileUpload]``
* ``uploadParamso("x")``: returns ``Option[List[FileUpload]]``

RequestVar
~~~~~~~~~~

To pass things around when processing a request (e.g. from action to view or layout)
in the typesafe way, you should use RequestVar.

Example:

Var.scala

::

  import xitrum.RequestVar

  object Var {
    object rTitle extends RequestVar[String]
  }

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {xitrumHead}
          <title>{if (Var.rTitle.isDefined) "My Site - " + Var.rTitle.get else "My Site"}</title>
        </head>
        <body>
          {renderedView}
        </body>
      </html>
    )
  }

ShowAction.scala

::

  class ShowAction extends AppAction {
    override def execute {
      val (title, body) = ...  // Get from DB
      Var.rTitle.set(title)
      renderView(body)
    }
  }

Cookie
------

TODO

Session
-------

SessionVar
~~~~~~~~~~

For example, you want save username to session after the user has logged in:

Declare the session var:

::

  import xitrum.SessionVar

  object Var {
    object sUsername extends SessionVar[String]
  }

After login success:

::

  Var.sUsername.set(username)

Display the username:

::

  if (Var.sUsername.isDefined)
    <em>{Var.sUsername.get}</em>
  else
    <a href={urlFor[LoginAction]}>Login</a>

* To delete the session var: ``Var.sUsername.delete``
* To reset the whole session: ``session.reset``

object vs. val
--------------

Please use ``object`` instead of ``val``.

**Do not do like this**:

::

  object Var {
    val rTitle    = new RequestVar[String]
    val rCategory = new RequestVar[String]

    val sUsername = new SessionVar[String]
    val sIsAdmin  = new SessionVar[Boolean]
  }

The above code compiles but does not work correctly, because the Vars internally
use class names to do look up. When using ``val``, ``rTitle`` and ``rCategory``
will have the same class name ("xitrum.RequestVar"). The same for ``sUsername``
and ``sIsAdmin``.

Session store
-------------

In config/xitrum.properties (`example <https://github.com/ngocdaothanh/xitrum/blob/master/plugin/src/main/resources/xitrum_resources/config/xitrum.properties>`_),
you can config the session store:

::

  session_store = xitrum.scope.session.CookieSessionStore

If you want to store session on server side using Hazelcast:

::

  session_store = xitrum.scope.session.HazelcastSessionStore

If you want to create your own session store, implement
`SessionStore <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/scope/session/SessionStore.scala>`_.
