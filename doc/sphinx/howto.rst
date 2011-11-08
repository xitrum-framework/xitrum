HOWTO
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s10.jpg

This chapter contains various small tips. Each tip is too small to have its own
chapter.

Basic authentication
--------------------

::

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

  libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.29"

This means that `Logback <http://logback.qos.ch/>`_ is used by default.
Logback config file is at ``config/logback.xml``.
You may replace Logback with any implementation of SLF4J.

Load config files
-----------------

Save your own config files in "config" directory. This directory is put into
classpath in development mode by build.sbt and in production mode by bin/runner.sh.

::

  import xitrum.util.Loader

  // Here you get an instance of java.util.Properties
  val properties = Loader.propertiesFromClasspath("myconfig.properties")
