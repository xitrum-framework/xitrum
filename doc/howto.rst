HOWTO
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s10.jpg

Serve static file from classpath resource (in .jar file)
--------------------------------------------------------

If you are a library developer, and want to serve myimage.png from your library.

Save the file in your .jar under:

::

  /public/path/to/myimage.png

Then in the web page, refer to it like this:

::

  <img src="/resources/public/path/to/myimage.png" />

Link to an action
-----------------

Xitrum tries to be typesafe.

Don't write URL manually, use urlFor like this:

::

  <a href={urlFor[ArticleShowAction]("id" -> myArticle.id)}>{myArticle.title}</a>

Load config files
-----------------

Save config files in "config" directory. This directory is put in classpath by
runner.sh, so that you can load them easily when your application is running.

To load in your application:

* xitrum.Config.load("file.txt"): returns the content of the file as a String
* xitrum.Config.loadProperties("file.properties"): returns a java.util.Properties
