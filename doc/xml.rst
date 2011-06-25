XML tips
========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s9.jpg

Scala allow wrting literal XML. Xitrum uses this feature as its "template engine":

* Scala checks XML syntax at compile time: Views are typesafe.
* Scala automatically escapes XML: Views are `XSS <http://en.wikipedia.org/wiki/Cross-site_scripting>`_-free by default.

Below are some tips.

Unescape XML
------------

Use ``scala.xml.Unparsed``:

::

  import scala.xml.Unparsed

  <script>
    {Unparsed("if (1 < 2) alert('Xitrum rocks');")}
  </script>

Or use ``<xml:unparsed>``:

::

  <script>
    <xml:unparsed>
      if (1 < 2) alert('Xitrum rocks');
    </xml:unparsed>
  </script>

``<xml:unparsed>`` will be hidden in the output:

::

  <script>
    if (1 < 2) alert('Xitrum rocks');
  </script>

Group XML elements
------------------

::

  <div id="header">
    {if (loggedIn)
      <xml:group>
        <b>{username}</b>
        <a href={urlFor[LogoutAction]}>Logout</a>
      </xml:group>
    else
      <xml:group>
        <a href={urlFor[LoginAction]}>Login</a>
        <a href={urlFor[RegisterAction]}>Register</a>
      </xml:group>}
  </div>

``<xml:group>`` will be hidden in the output, for example when the use has logged in:

::

  <div id="header">
    <b>My username</b>
    <a href="/login">Logout</a>
  </div>

Render XHTML
------------

Xitrum renders views and layouts as XHTML automatically.
If you want to render it yourself (rarely), pay attention to the code below.

::

  import scala.xml.Xhtml

  val br = <br />
  br.toString            // => <br></br>, some browsers will render this as 2 <br />s
  Xhtml.toXhtml(<br />)  // => "<br />"
