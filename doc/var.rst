RequestVar and SessionVar
=========================

RequestVar
----------

To pass things around when processing a request (e.g. from action to view or layout)
in the typesafe way, you should use RequestVar.

TODO

SessionVar
----------

Xitrum tries to be typesafe. SessionVar is a way to make your session more typesafe.

For example, you want save username to session after the user has logged in:

Declare the session var:

::

  import xitrum.action.env.session.SessionVar
  object svUsername extends SessionVar[String]

After login success:

::

  svUsername.set(username)

Display the username:

::

  if (svUsername.isDefined)
    <em>{svUsername.get}</em>
  else
    <a href={urlFor[LoginAction]}>Login</a>

* To delete the session var: ``svUsername.delete``
* To reset the whole session: ``session.reset``
