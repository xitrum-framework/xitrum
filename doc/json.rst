JSON
====

Jerkson is included in Xitrum as a dependecy:
https://github.com/codahale/jerkson

Please read about it to know how to parse and generate JSON.

To respond JSON:

::

  val scalaData = List(1, 2, 3)  // An example
  renderJson(scalaData)
