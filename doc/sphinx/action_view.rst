Action and View
===============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s7.jpg

What do you create web applications for? There are 2 main use cases:

* To serve machines: you need to create RESTful APIs for smartphones, web services
  for other web sites.
* To serve human users: you need to create interactive web pages.

As a web framework, Xitrum aims to support you to solve these use cases easily.
In Xitrum, there are 2 kinds of actions: :doc:`RESTful actions </restful>` and
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

Scalate
-------

For small views you can use Scala XML for convenience, but for big views you
should use `Scalate <http://scalate.fusesource.org/>`_.

scr/main/scala/quickstart/action/AppAction.scala:

::

  package quickstart.action

  import xitrum.Action

  trait AppAction extends Action {
    override def layout = renderScalateTemplateToString(classOf[AppAction])
  }

scr/main/scala/quickstart/action/IndexAction.scala:

::

  package quickstart.action

  import xitrum.annotation.GET

  @GET("/")
  class IndexAction extends AppAction {
    override def execute {
      renderScalateView()
    }

    def hello(what: String) = "Hello %s".format(what)
  }

scr/main/scalate/quickstart/action/AppAction.jade:

::

  !!! 5
  html
    head
      = antiCSRFMeta
      = xitrumCSS
      title Welcome to Xitrum

    body
      != renderedView
      = jsAtBottom

scr/main/scalate/quickstart/action/IndexAction.jade:

::

  - import quickstart.IndexAction

  a(href={urlForThis}) Path to current action
  p= helper.asInstanceOf[IndexAction].hello("World")

In views you can use all methods of the class `xitrum.Action <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/Action.scala>`_.
If you want to have exactly instance of the current action, cast ``helper`` to
the action you wish.
