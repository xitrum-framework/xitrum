JSON
====

Lift JSON is included in Xitrum as a dependecy:
https://github.com/lift/framework/tree/master/core/json

It is the best JSON implementation of Scala. Please read about it to know how
to parse, produce etc. JSON data.

To respond JSON data:

::

  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._

  val scalaData = List(1, 2, 3)  // An example
  renderText(compact(render(scalaData), "text/json")
