Validation
==========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s5.jpg

Xitrum includes `jQuery Validation plugin <http://bassistance.de/jquery-plugins/jquery-plugin-validation/>`_
and provides validation helpers for server side.

Default validators
------------------

Xitrum provides validators in ``xitrum.validator`` package.
They have these methods:

::

  v(name: String, value: Any): Option[String]
  e(name: String, value: Any)

If the validation check does not pass, ``v`` will return ``Some(error message)``,
``e`` will throw ``ValidationError(error message)``.

You can use validators anywhere you want.

Action example:

::

  import xitrum.validator._

  ...
  def create = POST("articles") {
    val title = param("tite")
    val body  = param("body")
    try {
      Required.e("Title", title)
      Required.e("Body",  body)
    } catch {
      case ValidationError(message) =>
        respondText(message)
        return
    }

    // Do with the valid title and body...
  }
  ...

If you don't ``try`` and ``catch``, when the validation check does not pass,
Xitrum will automatically catch the error message for you and respond it to the
requesting client. This is convenient when writing web APIs.

::

  val title = param("tite")
  Required.e("Title", title)
  val body  = param("body")
  Required.e("Body",  body)

Model example:

::

  import xitrum.validator._

  case class Article(id: Int = 0, title: String = "", body: String = "") {
    // Returns Some(error message) or None
    def v =
      // Chain validators together
      Required.v("Title", title) orElse
      Required.v("Body",  body)
  }

See `xitrum.validator pakage <https://github.com/ngocdaothanh/xitrum/tree/master/src/main/scala/xitrum/validator>`_
for the full list of default validators.

Write custom validators
-----------------------

Extend `xitrum.validator.Validator <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/validator/Validator.scala>`_.
You only have to implement ``v`` method. This method should returns Some(error message) or None.
