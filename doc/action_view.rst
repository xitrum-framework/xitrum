Action and View
===============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s7.jpg

Scala has XML feature built-in. Xitrum uses this feature as its "template engine":

* You don't have to learn another template engine syntax.
* Scala checks XML syntax at compile time => Views are typesafe.
* Scala automatically escapes XML => Views are XSS-free by default.

There are 2 kinds of actions: :doc:`RESTful actions </restful>` and :doc:`POSTback actions </postback>`.

Normally, you write view directly in its action.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class Index extends Action {
    def override execute {
      val s = "World"  // Will be automatically escaped

      renderView(
        <html>
          <body>
            <p>Hello <em>{s}</em>!</p>
          </body>
        </html>
      )
    }
  }

Of course you can refactor the view into a separate Scala file.

There are many other methods for rendering things other than views:

* renderText
* renderBinary
* renderFile

Async response
--------------

There is no default response. You must call renderXXX explicitly to send response
to the client. If you don't call renderXXX, the HTTP connection is kept for you,
and you can call renderXXX later.

Chunked response
----------------

TODO

Layout
------

You typically create a parent class which has a common layout for many views, like this:

``AppAction.scala``

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {xitrumHead}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
        </body>
      </html>
    )
  }

``xitrumHead`` includes jQuery, jQuery Validate plugin, default CSS etc.
In ``<head>``, it should be the first line, the following lines may override
things it includes if needed.

``Index.scala``

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class Index extends AppAction {
    def override execute {
      val s = "World"  // Will be automatically escaped
      renderView(<p>Hello <em>{s}</em>!</p>)
    }
  }

Unescape XML
------------

Use ``scala.xml.Unparsed``:

::

  import scala.xml.Unparsed

  <script>
    {Unparsed("if (1 < 2) alert('Xitrum rocks');")}
  </script>

Or use ``<xml:unparsed>``:

::

  <script>
    <xml:unparsed>
      if (1 < 2) alert('Xitrum rocks');
    </xml:unparsed>
  </script>

Group XML elements
------------------

::

  if (loggedIn)
    <xml:group>
      <b>{username}</b>
      <a href={urlFor[LogoutAction]}>Logout</a>
    </xml:group>
  else
    <xml:group>
      <a href={urlFor[LoginAction]}>Login</a>
      <a href={urlFor[RegisterAction]}>Register</a>
    </xml:group>

Render XHTML
------------

Xitrum renders views and layouts as XHTML automatically.
If you want to render it yourself (rarely), pay attention to the code below.

::

  import scala.xml.Xhtml

  val br = <br />
  br.toStirng            // => <br></br>, some browsers will render this as 2 <br />s
  Xhtml.toXhtml(<br />)  // => "<br />"
