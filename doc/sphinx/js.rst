JavaScript and JSON
===================

JavaScript
----------

Xitrum includes jQuery. There are some jsXXX helpers.

Add JavaScript fragments to view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In your action, call ``jsAddToView`` (multiple times if you need):

::

  class MyController extends AppController {
    def index = GET {
      ...
      jsAddToView("alert('Hello')")
      ...
      jsAddToView("alert('Hello again')")
      ...
      respondView(<p>My view</p>)
    }
  }

In your layout, call ``jsAtBottom``:

::

  import xitrum.Controller
  import xitrum.view.DocType

  trait AppController extends Controller {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
        </head>
        <body>
          <div id="flash">{jsFlash}</div>
          {renderedView}
          {jsAtBottom}
        </body>
      </html>
    )

Respond JavaScript directly without view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To respond JavaScript:

::

  jsRespond("$('#error').html(%s)".format(jsEscape(<p class="error">Could not login.</p>)))

To redirect:

::

  jsRedirectTo("http://cntt.tv/")
  jsRedirectTo(AuthenticateController.login)

JSON
----

Xitrum includes `Jerkson <https://github.com/codahale/jerkson>`_.
Please read about it to know how to parse and generate JSON.

To respond JSON:

::

  val scalaData = List(1, 2, 3)  // An example
  respondJson(scalaData)
