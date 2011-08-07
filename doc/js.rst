JavaScript and JSON
===================

JavaScript
----------

Xitrum includes jQuery. There are some jsXXX helpers.

Add JavaScript fragments to view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In your action, call ``jsAddToView`` (multiple times if you need):

::

  import xitrum.Action

  class MyAction extends AppAction {
    override def execute {
      ...
      jsAddToView("alert('Hello')")
      ...
      jsAddToView("alert('Hello again')")
      ...
      rederView(<p>My view</p>)
    }
  }

In your layout, call ``jsForView``:

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {xitrumHead}
        </head>
        <body>
          <div id="flash">{jsFlash}</div>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )

Respond JavaScript directly without view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To render JavaScript:

::

  jsRender("$('#error').html(%s)".format(jsEscape(<p class="error">Could not login.</p>)))
  or shorter:
  jsRenderFormat("$('#error').html(%s)", jsEscape(<p class="error">Could not login.</p>))

To redirect:

::

  jsRedirectTo("http://cntt.tv/")
  jsRedirectTo[LoginAction]

JSON
----

Xitrum includes `Jerkson <https://github.com/codahale/jerkson>`_.
Please read about it to know how to parse and generate JSON.

To respond JSON:

::

  val scalaData = List(1, 2, 3)  // An example
  renderJson(scalaData)
