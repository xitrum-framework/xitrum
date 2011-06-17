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

  import xitrum.action.Action
  import xitrum.action.annotation.GET

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

``ParentAction.scala``

::

  import xitrum.action.Action
  import xitrum.action.view.DocType

  trait ParentAction extends Action {
    override def layout = Some(() => DocType.xhtmlTransitional(
      <html>
        <body>
          {renderedView}
        </body>
      </html>
    ))

``Index.scala``

::

  import xitrum.action.Action
  import xitrum.action.annotation.GET

  @GET("/")
  class Index extends ParentAction {
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
