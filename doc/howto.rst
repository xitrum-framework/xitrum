HOWTO
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s10.jpg

This chapter contains various small tips. Each tip is too small to have its own
chapter.

Link to an action
-----------------

Xitrum tries to be typesafe.

Don't write URL manually, use urlFor like this:

::

  <a href={urlFor[ArticleShowAction]("id" -> myArticle.id)}>{myArticle.title}</a>

Log
---

Xitrum uses `SLF4J <http://www.slf4j.org/>`_. Your project must provide an
implementation of SLF4J.

When you create a new Xitrum project with ``sbt xitrum-new``, the created
build.sbt file has this line:

::

  libraryDependencies += "ch.qos.logback"  %  "logback-classic" % "0.9.29"

This means that `Logback <http://logback.qos.ch/>`_ (an implementation of SLF4J)
is used by default. Logback config file is at ``config/logback.xml``. You may
replace Logback with another implementation if you like.

Load config files
-----------------

Save config files in "config" directory. This directory is put in classpath by
runner.sh, so that you can load them easily when your application is running.

To load in your application:

* xitrum.Config.load("file.txt"): returns the content of the file as a String
* xitrum.Config.loadProperties("file.properties"): returns a java.util.Properties
