Validation
==========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s5.jpg

Xitrum includes `jQuery Validation plugin <http://bassistance.de/jquery-plugins/jquery-plugin-validation/>`_
and provides validation helpers for server side.

Default validators
------------------

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

Extend `xitrum.validator.Validator <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/validator/Validator.scala>`_
and implement ``v`` method. This method should returns Some(error message) or None.
