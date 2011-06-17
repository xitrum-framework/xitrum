Action and view
===============

To be flexible, Xitrum provides 2 kinds of actions:
normal action and actor action.

Normal action
-------------

Use when you don't do async call to outside from your action.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("hello")
  class HelloAction extends Action {
    def execute() {
      respondText("Hello")
    }
  }

With Action, the request is handled right away, but the number of concurrent
connections can't be too high. There should not be any blocking processing
along the way request -> response.

Actor action
------------

Use when you want to do async call to outside from your action.
If you want your action to be an actor, instead of extending xitrum.Action,
extend xitrum.ActionActor. With ActionActor, your system can handle massive
number of concurrent connections, but the request is not handled right away.
This is async oriented.

An actor instance will be created when there's request. It will be stopped when the
connection is closed or when the response has been sent by respondText,
respondView etc. methods. For chunked response, it is not stopped right away.
It is stopped when the last chunk is sent.

::

  import scala.concurrent.duration._

  import xitrum.ActionActor
  import xitrum.annotation.GET

  @GET("actor")
  class ActionActorDemo extends ActionActor with AppAction {
    // This is just a normal Akka actor

    def execute() {
      // See Akka doc about scheduler
      import context.dispatcher
      context.system.scheduler.scheduleOnce(3 seconds, self, System.currentTimeMillis)

      // See Akka doc about "become"
      context.become {
        case pastTime =>
          respondInlineView("It's " + pastTime + " Unix ms 3s ago.")
      }
    }
  }

Respond to client
-----------------

From an action, to respond something to client, use:

* ``respondView``: responds view template with or without layout
* ``respondInlineView``: responds with or without layout
* ``respondText("hello")``: responds a string without layout
* ``respondHtml("<html>...</html>")``: same as above, with content type set to "text/html"
* ``respondJson(List(1, 2, 3))``: converts Scala object to JSON object then responds
* ``respondJs("myFunction([1, 2, 3])")``
* ``respondJsonP(List(1, 2, 3), "myFunction")``: combination of the above two
* ``respondJsonText("[1, 2, 3]")``
* ``respondJsonPText("[1, 2, 3]", "myFunction")``
* ``respondBinary``: responds an array of bytes
* ``respondFile``: sends a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (aka send-file) is used
* ``respondEventSource("data", "event")``

Respond template view file
--------------------------

Each action may have an associated `Scalate <http://scalate.fusesource.org/>`_
template view file. Instead of responding directly in the action with the above
methods, you can use a separate view file.

scr/main/scala/mypackage/MyAction.scala:

::

  package mypackage

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("myAction")
  class MyAction extends Action {
    def execute() {
      respondView()
    }

    def hello(what: String) = "Hello %s".format(what)
  }

scr/main/scalate/mypackage/MyAction.jade:

::

  - import mypackage.MyAction

  !!! 5
  html
    head
      != antiCSRFMeta
      != xitrumCSS
      != jsDefaults
      title Welcome to Xitrum

    body
      a(href={url}) Path to the current action
      p= currentAction.asInstanceOf[MyAction].hello("World")

      != jsForView

* ``xitrumCSS`` includes the default CSS for Xitrum. You may remove it if you
  don't like.
* ``jsDefaults`` includes jQuery, jQuery Validate plugin etc.
  should be put at layout's <head>.
* ``jsForView`` contains JS fragments added by ``jsAddToView``,
  should be put at layout's bottom.

In templates you can use all methods of the class `xitrum.Action <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/Action.scala>`_.
Also, you can use utility methods provided by Scalate like ``unescape``.
See the `Scalate doc <http://scalate.fusesource.org/documentation/index.html>`_.

If you want to have exactly instance of the current action, cast ``currentAction`` to
the action you wish.

The default Scalate template type is `Jade <http://scalate.fusesource.org/documentation/jade.html>`_.
You can also use `Mustache <http://scalate.fusesource.org/documentation/mustache.html>`_,
`Scaml <http://scalate.fusesource.org/documentation/scaml-reference.html>`_, or
`Ssp <http://scalate.fusesource.org/documentation/ssp-reference.html>`_.
To config the default template type, see xitrum.conf file in the config directory
of your Xitrum application.

You can override the default template type by passing "jade", "mustache", "scamal",
or "ssp" to `respondView`.

::

  respondView(Map("type" ->"mustache"))

Mustache
~~~~~~~~

Must read:

* `Mustache syntax <http://mustache.github.com/mustache.5.html>`_
* `Scalate implementation <http://scalate.fusesource.org/documentation/mustache.html>`_

You can't do some things with Mustache like with Jade, because Mustache syntax
is stricter.

To pass things from action to Mustache template, you must use ``at``:

Action:

::

  at("name") = "Jack"
  at("xitrumCSS") = xitrumCSS

Mustache template:

::

  My name is {{name}}
  {{xitrumCSS}}

Note that you can't use the below keys for ``at`` map to pass things to Scalate
template, because they're already used:

* "context": for Sclate utility object, which contains methods like ``unescape``
* "helper": for the current action object

CoffeeScript
~~~~~~~~~~~~

You can embed CoffeeScript in Scalate template using
`:coffeescript filter <http://scalate.fusesource.org/documentation/jade-syntax.html#filters>`_:

::

  body
    :coffeescript
      alert "Hello, Coffee!"

Output:

::

  <body>
    <script type='text/javascript'>
      //<![CDATA[
        (function() {
          alert("Hello, Coffee!");
        }).call(this);
      //]]>
    </script>
  </body>

But note that it is `slow <http://groups.google.com/group/xitrum-framework/browse_thread/thread/6667a7608f0dc9c7>`_:

::

  jade+javascript+1thread: 1-2ms for page
  jade+coffesscript+1thread: 40-70ms for page
  jade+javascript+100threads: ~40ms for page
  jade+coffesscript+100threads: 400-700ms for page

You pre-generate CoffeeScript to JavaScript if you need speed.

Layout
------

When you respond a view with ``respondView`` or ``respondInlineView``, Xitrum
renders it to a String, and sets the String to ``renderedView`` variable. Xitrum
then calls ``layout`` method of the current action, finally Xitrum responds
the result of this method to the browser.

By default ``layout`` method just returns ``renderedView`` itself. If you want
to decorate your view with something, override this method. If you include
``renderedView`` in the method, the view will be included as part of your layout.

The point is ``layout`` is called after your action's view, and whatever returned
is what responded to the browser. This mechanism is simple and straight forward.
No magic. For convenience, you may think that there's no layout in Xitrum at all.
There's just the ``layout`` method and you do whatever you want with it.

Typically, you create a parent class which has a common layout for many views:

src/main/scala/mypackage/AppAction.scala

::

  package mypackage
  import xitrum.Action

  trait AppAction extends Action {
    override def layout = renderViewNoLayout(classOf[AppAction])
  }

src/main/scalate/mypackage/AppAction.jade

::

  !!! 5
  html
    head
      != antiCSRFMeta
      != xitrumCSS
      != jsDefaults
      title Welcome to Xitrum

    body
      != renderedView
      != jsForView

src/main/scala/mypackage/MyAction.scala

::

  package mypackage
  import xitrum.annotation.GET

  @GET("myAction")
  class MyAction extends AppAction {
    def execute() {
      respondView()
    }

    def hello(what: String) = "Hello %s".format(what)
  }

scr/main/scalate/mypackage/MyAction.jade:

::

  - import mypackage.MyAction

  a(href={url}) Path to the current action
  p= currentAction.asInstanceOf[MyAction].hello("World")

Without separate layout file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )
  }

Pass layout directly in respondView
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  val specialLayout = () =>
    DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          {jsDefaults}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )

  respondView(specialLayout _)

Inline view
-----------

Normally, you write view in a Scalate file. You can also write it directly:

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("myAction")
  class MyAction extends Action {
    def execute() {
      val s = "World"  // Will be automatically escaped
      respondInlineView(
        <p>Hello <em>{s}</em>!</p>
      )
    }
  }

Render fragment
---------------

If you want to render the frament file
scr/main/scalate/mypackage/MyAction/_myfragment.jade:

::

  renderFragment(classOf[MyAction], "myfragment")

If MyAction is the current action, you can skip it:

::

  renderFragment("myfragment")
