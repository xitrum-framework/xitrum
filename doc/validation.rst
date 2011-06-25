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

Default validators
------------------

Xitrum provides a lot of default validators. You can use them right away.

::

  // See this package for a complete list of avaiable default validators
  // https://github.com/ngocdaothanh/xitrum/tree/master/src/main/scala/xitrum/validation
  import xitrum.validation._

  <form postback="submit" action={urlForPostbackThis}>
    Username:
    <input type="text" name={validate("username", MinLength(5), MaxLength(10)} /><br />

    Password:
    <input type="password" name={validate("password", Required)} /><br />

    Password confirmation:
    <input type="passord" name={validate("password_confirm", EqualTo("password"))} /><br />

    Memo:
    <textarea name={validate("memo")}></textarea><br />

    <input type="submit" value="Register" />
  </form>

Names of inputs will be encrypted to include the serialized validators. They will
be automatically decrypted when the form is posted back to the server.

**Note**

  Technically, for the server side to ensure that hackers cannot bypass validators,
  all input names must be encrypted. This means that even inputs that do not need
  validation must be marked with ``validate``. See ``validate("memo")`` above.
  We will try to remove this is inconvenience in the next version of Xitrum.

Write custom validators
-----------------------

You can also write your own custom validators very easily.

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
