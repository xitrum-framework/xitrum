Static files
============

.. image:: img/be_ti_xitrum_baby_smurf.jpg

Serve file from public directory
--------------------------------

Project directory layout:

::

  config
  public
    favicon.ico
    robots.txt
    img
      myimage.png
    css
      mystyle.css
  src
  build.sbt

Xitrum can serve files from the public directory. But URLs to them must have
prefix ``/public/``:

::

  /public/img/myimage.png
  /public/css/mystyle.css

This is the design decision of Xitrum for speed, to avoid checking file
existence on every request.

To refer to them in your source code:

::

  <img src={urlForPublic("img/myimage.png")} />

publicFilesNotBehindPublicUrl
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

favicon.ico and robots.txt are special files, their URLs:

::

  /favicon.ico
  /robots.txt

When for example you have a file called crossdomain.xml and you want its
URL to be ``http://mydomain.com/crossdomain.xml`` (no ``/public/`` prefix),
put it right in the ``public`` directory, and in your boot class add this line:

::

  xitrum.Config.publicFilesNotBehindPublicUrl ++= List("/crossdomain.xml")

Serve file from resource in .jar file in classpath
--------------------------------------------------

If you are a library developer, and want to serve myimage.png from your library.

Save the file in your .jar under ``public`` directory:

::

  public/my_lib/img/myimage.png

To refer to them in your source code:

::

  <img src={urlForResource("my_lib/img/myimage.png")} />
