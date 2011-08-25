Action and View
===============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s7.jpg

There are 2 kinds of actions: :doc:`RESTful actions </restful>` and
:doc:`postback actions </postback>`.

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

There are methods for rendering things other than views:

* ``renderText``: renders anything as a string without layout
* ``renderJson``: renders JSON
* ``renderBinary``: renders an array of bytes
* ``renderFile``: sends a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (send-file) is used

Layout
------

With ``renderView``, layout is rendered. By default the layout is what passed to
``renderView``. You can customize the layout by overriding the ``layout`` method.

Typically, you create a parent class which has a common layout for many views,
like this:

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
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsAtBottom}
        </body>
      </html>
    )
  }

``xitrumCSS`` includes the default CSS for Xitrum. You may remove it if you
don't like.

``jsAtBottom`` includes jQuery, jQuery Validate plugin etc.

Index.scala

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class Index extends AppAction {
    def override execute {
      val s = "World"
      renderView(<p>Hello <em>{s}</em>!</p>)
    }
  }

You can pass the layout directly to ``renderView``:

::

  val specialLayout = () =>
    <html>
      <body>
        {renderedView}
      </body>
    </html>

  val s = "World"
  renderView(<p>Hello <em>{s}</em>!</p>, specialLayout _)
