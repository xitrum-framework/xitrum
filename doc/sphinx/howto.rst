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

Encrypt data
------------

To encrypt data that you don't need to decrypt later (one way encryption),
you can use MD5 or something like that.

If you want to decrypt later, you can use the utility Xitrum provides:

::

  import xitrum.util.Secure
  val encrypted: Array[Byte]         = Secure.encrypt("my data".getBytes)
  val decrypted: Option[Array[Byte]] = Secure.decrypt(encrypted)

You can use ``xitrum.util.Base64`` to encode and decode the binary data to
normal string (to embed to HTML for response etc.).

If you can combine the above operations in one step:

::

  import xitrum.util.SecureBase64
  val encrypted: String         = SecureBase64.encrypt("my object")
  val decrypted: Option[String] = SecureBase64.decrypt(encrypted).asInstanceOf[Option[String]]

``SecureBase64`` uses ``xitrum.util.SeriDeseri`` to serialize and deserialize.
As a result, your data must be serializable.

You can specify a key for encryption and decryption, like:

::

  Secure.encrypt("my data".getBytes, "my key")
  Secure.decrypt(encrypted, "my key")

  SecureBase64.encrypt("my object", "my key")
  SecureBase64.decrypt(encrypted, "my key")

If no key is specified, ``secureKey`` in xitrum.json file in config directory
is used.
