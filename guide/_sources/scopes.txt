Scopes
======

Request
-------

Kinds of params
~~~~~~~~~~~~~~~

There are 2 kinds of request params: textual params and file upload params (binary).

There are 3 kinds of textual params, of type ``scala.collection.mutable.Map[String, List[String]]``:

1. ``uriParams``: params after the ? mark in the URL, example: http://example.com/blah?x=1&y=2
2. ``bodyParams``: params in POST request body
3. ``pathParams``: params embedded in the URL, example: ``GET("articles/:id/:title")``

These params are merged in the above order as ``textParams``
(from 1 to 3, the latter will override the former).

``fileUploadParams`` is of type scala.collection.mutable.Map[String, List[`FileUpload <http://netty.io/3.6/api/org/jboss/netty/handler/codec/http/multipart/FileUpload.html>`_]].

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
For more details, see :doc:`Upload chapter </upload>`.

"at"
~~~~

To pass things around when processing a request (e.g. from action to view or layout)
you can use ``at``. ``at`` type is ``scala.collection.mutable.HashMap[String, Any]``.
If you know Rails, you'll see ``at`` is a clone of ``@`` of Rails.

Articles.scala

::

  @GET("articles/:id")
  class ArticlesShow extends AppAction {
    def execute() {
      val (title, body) = ...  // Get from DB
      at("title") = title
      respondInlineView(body)
    }
  }

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          {jsDefaults}
          <title>{if (at.isDefinedAt("title")) "My Site - " + at("title") else "My Site"}</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )
  }

RequestVar
~~~~~~~~~~

``at`` in the above section is not typesafe because you can set anything to the
map. To be more typesafe, you should use RequestVar, which is a wrapper arround
``at``.

RVar.scala

::

  import xitrum.RequestVar

  object RVar {
    object title extends RequestVar[String]
  }

Articles.scala

::

  @GET("articles/:id")
  class ArticlesShow extends AppAction {
    def execute() {
      val (title, body) = ...  // Get from DB
      RVar.title.set(title)
      respondInlineView(body)
    }
  }

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          {jsDefaults}
          <title>{if (RVar.title.isDefined) "My Site - " + RVar.title.get else "My Site"}</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )
  }

Cookie
------

`Read Wikipedia about cookie path etc. <http://en.wikipedia.org/wiki/HTTP_cookie#Domain_and_Path>`_

Inside an action, use ``requestCookies``, a ``Map[String, String]``, to read cookies sent by browser.

::

  requestCookies.get("myCookie") match {
    case None         => ...
    case Some(string) => ...
  }

To send cookie to browser, create an instance of `DefaultCookie <http://netty.io/3.6/api/org/jboss/netty/handler/codec/http/DefaultCookie.html>`_
and append it to ``responseCookies``, an ``ArrayBuffer`` that contains `Cookie <http://netty.io/3.6/api/org/jboss/netty/handler/codec/http/Cookie.html>`_.

::

  val cookie = new DefaultCookie("name", "value")
  cookie.setHttpOnly(true)  // true: JavaScript cannot access this cookie
  responseCookies.append(cookie)

If you don't set cookie's path by calling ``cookie.setPath(cookiePath)``, its
path will be set to the site's root path (``xitrum.Config.withBaseUrl("/")``).
This avoids accidental duplicate cookies.

To delete a cookie sent by browser, send a cookie with the same name and set
its max age to 0. The browser will expire it immediately. To tell browser to
delete cookie when the browser closes windows, set max age to ``Integer.MIN_VALUE``:

::

  cookie.setMaxAge(Integer.MIN_VALUE)

Note that `Internet Explorer does not support "max-age" <http://mrcoles.com/blog/cookies-max-age-vs-expires/>`_,
but Netty detects and outputs either "max-age" or "expires" properly. Don't worry!

If you want to sign your cookie value to prevent user from tampering, use
``xitrum.util.SecureUrlSafeBase64.encrypt`` and ``xitrum.util.SecureUrlSafeBase64.encrypt``.
For more information, see :doc:`How to encrypt data </howto>`.

Session
-------

Session storing, restoring, encrypting etc. is done automatically by Xitrum.
You don't have to mess with them.

In your actions, you can use ``session``. It is an instance of
``scala.collection.mutable.Map[String, Any]``. Things in ``session`` must be
serializable.

For example, to mark that a user has logged in, you can set his username into the
session:

::

  session("userId") = userId

Later, if you want to check if a user has logged in or not, just check if
there's a username in his session:

::

  if (session.isDefinedAt("userId")) println("This user has logged in")

Storing user ID and pull the user from database on each access is usually a good
practice. That way changes to the user are updated on each access (including
changes to user roles/authorizations).

session.clear()
~~~~~~~~~~~~~~~

`One line of code will protect you from session fixation <http://guides.rubyonrails.org/security.html#session-fixation>`_.

Read the link above to know about session fixation. To prevent session fixation
attack, in the action that lets users login, call ``session.clear()``.

::

  @GET("login")
  class LoginAction extends Action {
    def execute() {
      ...
      session.clear()  // Reset first before doing anything else with the session
      session("userId") = userId
    }
  }

To log users out, also call ``session.clear()``.

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
    <a href={url[LoginAction]}>Login</a>

* To delete the session var: ``SVar.username.delete()``
* To reset the whole session: ``session.clear()``

Session store
~~~~~~~~~~~~~

In config/xitrum.conf (`example <https://github.com/ngocdaothanh/xitrum/blob/master/plugin/src/main/resources/xitrum_resources/config/xitrum.conf>`_),
you can config the session store:

::

  ...
  session {
    # Store sessions on client side
    store = xitrum.scope.session.CookieSessionStore

    # Store sessions on server side
    #store = xitrum.scope.session.HazelcastSessionStore
    #store = xitrum.scope.session.UnserializableSessionStore

    # If you run multiple sites on the same domain, make sure that there's no
    # cookie name conflict between sites
    cookieName = _session

    # Key to encrypt session cookie etc.
    # Do not use the example below! Use your own!
    # If you deploy your application to several instances be sure to use the same key!
    secureKey = ajconghoaofuxahoi92chunghiaujivietnamlasdoclapjfltudoil98hanhphucup8
  }
  ...

Server side session store is recommended when using continuations-based actions,
since serialized continuations are usually too big to store in cookies. 

HazelcastSessionStore is cluster-aware, but things you store in it must be serializable.

If you must store unserializable things, use UnserializableSessionStore.
If you use UnserializableSessionStore and still want to run a Xitrum cluster,
you must use a load balancer with sticky sessions configured.

The three default session stores above are enough for normal cases.
But if you have a special case and want to implement your own session store,
extend
`SessionStore <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/session/SessionStore.scala>`_
or
`ServerSessionStore <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/session/ServerSessionStore.scala>`_
and implement the abstract methods.

Then to tell Xitrum to use your session store, set its class name to xitrum.conf.

Good read:
`Web Based Session Management - Best practices in managing HTTP-based client sessions <http://www.technicalinfo.net/papers/WebBasedSessionManagement.html>`_.

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
