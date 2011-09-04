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

These params are merged in the above order as ``textParams``
(from 1 to 3, the latter will override the former).

``fileUploadParams`` is of type scala.collection.mutable.Map[String, List[`FileUpload <https://github.com/netty/netty/blob/master/src/main/java/org/jboss/netty/handler/codec/http/FileUpload.java>`_]].

Accesing params
~~~~~~~~~~~~~~~

From an action, you can access the above params directly, or you can use
accessor methods.

To access ``textParams``:

* ``param("x")``: returns ``String``, throws exception if x does not exist
* ``params("x")``: returns ``List[String]``, throws exception if x does not exist
* ``paramo("x")``: returns ``Option[String]``
* ``paramso("x")``: returns ``Option[List[String]]``

You can convert text params to other types (Int, Long, Fload, Double) automatically
by using ``param[Int]("x")``, ``params[Int]("x")`` etc. To convert text params to more types,
override `convertText <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/scope/request/ParamAccess.scala>`_.

For file upload: ``param[FileUpload]("x")``, ``params[FileUpload]("x")`` etc.

RequestVar
~~~~~~~~~~

To pass things around when processing a request (e.g. from action to view or layout)
in the typesafe way, you should use RequestVar.

RVar.scala

::

  import xitrum.RequestVar

  object RVar {
    object title extends RequestVar[String]
  }

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          <title>{if (RVar.title.isDefined) "My Site - " + RVar.title.get else "My Site"}</title>
        </head>
        <body>
          {renderedView}
          {jsAtBottom}
        </body>
      </html>
    )
  }

ShowAction.scala

::

  class ShowAction extends AppAction {
    override def execute {
      val (title, body) = ...  // Get from DB
      RVar.title.set(title)
      renderView(body)
    }
  }

Cookie
------

TODO

Session
-------

In your actions, you can use ``session``. It is an instance of
``scala.collection.mutable.Map[String, Any]``. Things in ``session`` must be
serializable.

resetSession
~~~~~~~~~~~~

`One line of code will protect you from session fixation <http://guides.rubyonrails.org/security.html#session-fixation>`_.

Read the link above to know about session fixation. To prevent session fixation
attack, in the action that lets users login, call ``resetSession``.

::

  class LoginAction extends Action {
    override def execute {
      ...
      resetSession  // Reset first before doing anything else with the session
      session("username") = username
    }
  }

To log users out, also call ``resetSession``.

SessionVar
~~~~~~~~~~

SessionVar, like RequestVar, is a way to make your session more typesafe.

For example, you want save username to session after the user has logged in:

Declare the session var:

::

  import xitrum.SessionVar

  object SVar {
    object username extends SessionVar[String]
  }

After login success:

::

  SVar.username.set(username)

Display the username:

::

  if (SVar.username.isDefined)
    <em>{SVar.username.get}</em>
  else
    <a href={urlFor[LoginAction]}>Login</a>

* To delete the session var: ``SVar.username.delete``
* To reset the whole session: ``resetSession``

Session store
~~~~~~~~~~~~~

In config/xitrum.properties (`example <https://github.com/ngocdaothanh/xitrum/blob/master/plugin/src/main/resources/xitrum_resources/config/xitrum.properties>`_),
you can config the session store:

::

  session_store = xitrum.scope.session.CookieSessionStore

If you want to store session on server side using Hazelcast:

::

  session_store = xitrum.scope.session.HazelcastSessionStore

You may need to setup session replication by :doc:`configuring Hazelcast </cluster>`.

If you want to create your own session store, implement
`SessionStore <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/scope/session/SessionStore.scala>`_.

object vs. val
--------------

Please use ``object`` instead of ``val``.

**Do not do like this**:

::

  object RVar {
    val title    = new RequestVar[String]
    val category = new RequestVar[String]
  }

  object SVar {
    val username = new SessionVar[String]
    val isAdmin  = new SessionVar[Boolean]
  }

The above code compiles but does not work correctly, because the Vars internally
use class names to do look up. When using ``val``, ``title`` and ``category``
will have the same class name "xitrum.RequestVar". The same for ``username``
and ``isAdmin``.
