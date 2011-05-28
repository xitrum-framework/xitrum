Validation
==========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s5.jpg

Xitrum integrates with jQuery Validation plugin:
http://bassistance.de/jquery-plugins/jquery-plugin-validation/

Validation is performed on both browser side (by jQuery Validation plugin) and
server side.

Default validators
------------------

::

  // See this package for a complete list of avaiable default validators
  import xitrum.validation._

  <form postback="submit">
    {<input type="text" name="username" />.validate(new Required, new MinLength(5), new MaxLength(10)}
    {<input type="password" name="password" />.validate(new Required)}
    {<input type="passord_confirm" name="password_confirm" />.validate(new PasswordConfirm("password"))}
  </form>

Write custom validators
-----------------------

::

  import xitrum.validation.Validator

  class MyValidator extends Validator {
    // Client side validation
    //
    // This method should output JS that uses jQuery Validation plugin to validate securedParamName
    def render(action: Action, paramName: String, securedParamName: String) {
      // Call jsAddToView(<JS that uses jQuery Validation plugin>)
      // to add JS to the end of the web page
    }

    // Server side validation
    def validate(action: Action, paramName: String, securedParamName: String): Boolean = {
    }
  }
