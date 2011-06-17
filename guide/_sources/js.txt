JavaScript and JSON
===================

JavaScript
----------

Xitrum includes jQuery. There are some jsXXX helpers.

Add JavaScript fragments to view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In your action, call ``jsAddToView`` (multiple times if you need):

::

  class MyAction extends AppAction {
    def execute() {
      ...
      jsAddToView("alert('Hello')")
      ...
      jsAddToView("alert('Hello again')")
      ...
      respondView(<p>My view</p>)
    }
  }

In your layout, call ``jsForView``:

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

To respond JavaScript:

::

  jsRespond("$('#error').html(%s)".format(jsEscape(<p class="error">Could not login.</p>)))

To redirect:

::

  jsRedirectTo("http://cntt.tv/")
  jsRedirectTo[LoginAction]()

JSON
----

Xitrum includes `JSON4S <https://github.com/json4s/json4s>`_.
Please read about it to know how to parse and generate JSON.

To convert between Scala case object and JSON string:

::

  import xitrum.util.Json

  case class Person(name: String, age: Int, phone: Option[String])
  val person1 = Person("Jack", 20, None)
  val json    = Json.generate(person)
  val person2 = Json.parse(json)

To respond JSON:

::

  val scalaData = List(1, 2, 3)  // An example
  respondJson(scalaData)

JSON is also neat for config files that need nested structures.
See :doc:`Load config files </howto>`.
