Deploy to production server
===========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

You may run Xitrum directly:

::

  Browser ------ Xitrum instance

Or behind a proxy server like Apache or Nginx:

::

  Browser ------ Proxy ------ Xitrum instance

Package directory
-----------------

Run ``sbt xitrum-package`` to prepare ``target/xitrum`` directory, ready to
deploy to production server:

::

  target/xitrum
    bin
      runner.sh
    config
      [config files]
    public
      [static public files]
    lib
      [dependencies and packaged project file]

Customize xitrum-package
------------------------

By default ``sbt xitrum-package`` command simply copies ``config`` and ``public``
directories to ``target/xitrum``. If you want it to copy additional files
and directories (README, INSTALL, doc etc.), config ``build.sbt`` like this:

::

  TODO

Starting Xitrum in production mode
----------------------------------

``bin/runner.sh`` is the script to run any object with ``main`` method. Use it to
start the web server in production environment.

::

  $ bin/runner.sh quickstart.Boot

You may want to modify runner.sh to tune JVM settings. Also see ``config/xitrum.properties``.

To start Xitrum in background when the system starts, `daemontools <http://cr.yp.to/daemontools.html>`_
is a very good tool. To install it on CentOS, see
`this instruction <http://whomwah.com/2008/11/04/installing-daemontools-on-centos5-x86_64/>`_.
