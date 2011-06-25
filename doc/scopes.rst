Scopes
======

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s11.jpg

To pass things around, there are 2 scopes: request and session. Xitrum tries to
be typesafe. RequestVar and SessionVar is a way to achieve that goal.

RequestVar
----------

To pass things around when processing a request (e.g. from action to view or layout)
in the typesafe way, you should use RequestVar.

Example:

Var.scala

::

  import xitrum.RequestVar

  object Var {
    object rTitle extends RequestVar[String]
  }

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
      <html>
        <head>
          {xitrumHead}
          <title>{if (Var.rTitle.isDefined) "My Site - " + Var.rTitle.get else "My Site"}</title>
        </head>
        <body>
          {renderedView}
        </body>
      </html>
    )
  }

ShowAction.scala

::

  class ShowAction extends AppAction {
    override def execute {
      val (title, body) = ...  // Get from DB
      Var.rTitle.set(title)
      renderView(body)
    }
  }

SessionVar
----------

For example, you want save username to session after the user has logged in:

Declare the session var:

::

  import xitrum.SessionVar

  object Var {
    object sUsername extends SessionVar[String]
  }

After login success:

::

  Var.sUsername.set(username)

Display the username:

::

  if (Var.sUsername.isDefined)
    <em>{Var.sUsername.get}</em>
  else
    <a href={urlFor[LoginAction]}>Login</a>

* To delete the session var: ``Var.sUsername.delete``
* To reset the whole session: ``session.reset``

object vs. val
--------------

Please use ``object`` instead of ``val``.

**Do not do like this**:

::

  object Var {
    val rTitle    = new RequestVar[String]
    val rCategory = new RequestVar[String]

    val sUsername = new SessionVar[String]
    val sIsAdmin  = new SessionVar[Boolean]
  }

The above code compiles but does not work correctly, because the Vars internally
use class names to do look up. When using ``val``, ``rTitle`` and ``rCategory``
will have the same class name ("xitrum.RequestVar"). The same for ``sUsername``
and ``sIsAdmin``.
