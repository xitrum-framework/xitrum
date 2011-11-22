HOWTO
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s10.jpg

This chapter contains various small tips. Each tip is too small to have its own
chapter.

Determine is the request is Ajax request
----------------------------------------

Use ``isAjax``.

::

  // In an action
  val msg = "A message"
  if (isAjax)
    jsRender("alert(" + jsEscape(msg) + ")")
  else
    renderText(msg)

Basic authentication
--------------------

::

  import xitrum.Action

  class MyAction extends Action {
    beforeFilters("authenticate") = basicAuthenticate("Realm") { (username, password) =>
      username == "myusername" && password == "mypassword"
    }
  }

Link to an action
-----------------

Xitrum tries to be typesafe.

Don't write URL manually, use urlFor like this:

::

  <a href={urlFor[ArticleShowAction]("id" -> myArticle.id)}>{myArticle.title}</a>

Log
---

Xitrum actions extend trait xitrum.Logger, which provides ``logger``.
In any action, you can do like this:

::

  logger.debug("Hello World")

Of course you can extend xitrum.Logger any time you want:

::

  object MyModel extends xitrum.Logger {
    ...
  }

In build.sbt, notice this line:

::

  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.0"

This means that `Logback <http://logback.qos.ch/>`_ is used by default.
Logback config file is at ``config/logback.xml``.
You may replace Logback with any implementation of SLF4J.

Load config files
-----------------

Save your own config files in "config" directory. This directory is put into
classpath in development mode by build.sbt and in production mode by bin/runner.sh.

myconfig.json

::

  {
    // You can write comment in JSON file like this
    "username": "God",
    // Keys must be quoted with double quotes
    "password": "Does God needs a password?",
    "children": ["Adam", "Eva"]
  }

Load it:

::

  case class MyConfig(username: String, password: String, children: List[String])
  val myConfig = xitrum.util.Loader.jsonFromClasspath[MyConfig]("myconfig.json")

You can also use properties files, but you should use JSON whenever possible
because it's much better. Properties files are not typesafe, do not support UTF-8
and nested structures etc.

myconfig.properties

::

  username = God
  password = Does God needs a password?
  children = Adam, Eva

::

  // Here you get an instance of java.util.Properties
  val properties = xitrum.util.Loader.propertiesFromClasspath("myconfig.properties")
