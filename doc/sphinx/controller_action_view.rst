Controller, action, and view
============================

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

  import xitrum.Controller

  class MyController extends Controller {
    def index = GET {
      val s = "World"  // Will be automatically escaped

      respondInlineView(
        <html>
          <body>
            <p>Hello <em>{s}</em>!</p>
          </body>
        </html>
      )
    }
  }

Of course you can refactor the view into a separate Scala file.

There are methods for responding things other than views:

* ``respondText``: responds anything as a string without layout
* ``respondJson``: responds JSON
* ``respondBinary``: responds an array of bytes
* ``respondFile``: sends a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (send-file) is used

Layout
------

With ``respondInlineView``, layout is rendered. By default the layout is what passed to
``respondInlineView``. You can customize the layout by overriding the ``layout`` method.

Typically, you create a parent class which has a common layout for many views,
like this:

AppController.scala

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
          {respondedView}
          {jsAtBottom}
        </body>
      </html>
    )
  }

``xitrumCSS`` includes the default CSS for Xitrum. You may remove it if you
don't like.

``jsAtBottom`` includes jQuery, jQuery Validate plugin etc.

MyController.scala

::

  import xitrum.Controller

  class MyController extends AppController {
    def index = GET {
      val s = "World"
      respondInlineView(<p>Hello <em>{s}</em>!</p>)
    }
  }

You can pass the layout directly to ``respondInlineView``:

::

  val specialLayout = () =>
    <html>
      <body>
        {respondedView}
      </body>
    </html>

  val s = "World"
  respondInlineView(<p>Hello <em>{s}</em>!</p>, specialLayout _)

Scalate
-------

For small views you can use Scala XML for convenience, but for big views you
should use `Scalate <http://scalate.fusesource.org/>`_.

scr/main/scala/quickstart/controller/AppController.scala:

::

  package quickstart.controller

  import xitrum.Controller

  trait AppController extends Controller {
    override def layout = renderScalate(classOf[AppAction])
  }

scr/main/scala/quickstart/action/MyController.scala:

::

  package quickstart.controller

  class MyController extends AppController {
    def index = GET {
      respondView()
    }

    def hello(what: String) = "Hello %s".format(what)
  }

scr/main/scalate/quickstart/controller/AppController.jade:

::

  !!! 5
  html
    head
      = antiCSRFMeta
      = xitrumCSS
      title Welcome to Xitrum

    body
      != respondedView
      = jsAtBottom

scr/main/scalate/quickstart/controller/MyController/index.jade:

::

  - import quickstart.controller.MyController

  a(href={currentAction.url}) Path to current action
  p= currentController.asInstanceOf[MyController].hello("World")

In views you can use all methods of the class `xitrum.Controller <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/Controller.scala>`_.
If you want to have exactly instance of the current controller, cast ``currentController`` to
the controller you wish.

The default Scalate template type is `Jade <http://scalate.fusesource.org/documentation/jade.html>`_.
You can also use `Mustache <http://scalate.fusesource.org/documentation/mustache.html>`_,
`Scaml <http://scalate.fusesource.org/documentation/scaml-reference.html>`_, or
`Ssp <http://scalate.fusesource.org/documentation/ssp-reference.html>`_.
To config the default template type, see `scalate` in xitrum.json.

You can override the default template type by passing "jade", "mustache", "scamal",
or "ssp" as the last parameter to `renderScalate` or `respondView`.

::

  renderScalate(classOf[AppAction], "mustache")
  respondView("scaml")
