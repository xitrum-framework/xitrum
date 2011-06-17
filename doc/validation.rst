Validation
==========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s5.jpg

Xitrum is not a full-stack web framework like `Rails <http://rubyonrails.org/>`_
because it does not provide M (in MVC). However, it does provide validation
feature at Action (Controller) and View. You can use any database access library
you want, while still can validate user input easily.

Xitrum integrates with `jQuery Validation plugin <http://bassistance.de/jquery-plugins/jquery-plugin-validation/>`_.
Validation is performed at both browser side and server side.

::

  User input -> Validation at View (browser side) -> Validation before Action (server side) -> Action
                              |                                         |
                            FAILED                                    FAILED
                       <------+                                  <------+


Xitrum provides a lot of default validators. You can also write your own custom
validators very easily.

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
