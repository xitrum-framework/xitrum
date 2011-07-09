Deploy to production server
===========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

You may run Xitrum directly:

::

  Browser ------ Xitrum instance

Or behind a proxy server like Apache or Nginx:

  Browser ------ Proxy ------ Xitrum instance

Package directory
-----------------

Run ``sbt xitrum-package`` to prepare ``target/xitrum_package`` directory,
ready to deploy to production server:

::

  target/xitrum_package
    bin
      runner.sh
    config
      [config files]
    static
      [static public files]
    lib
      [dependencies and packaged project file]

Copy additional files to target/xitrum_package
----------------------------------------------

``sbt xitrum-package`` command simply copies ``config`` and ``public``
directories to ``target/xitrum_package``. If you want it to copy additional files
and directories (README, INSTALL, doc etc.), config ``build.sbt`` like this:

::

  TODO

Now run ``sbt xitrum-package`` and check ``target/xitrum_package`` directory.

Starting Xitrum in production mode
----------------------------------

runner.sh is the script to start the web server in production environment.
You may want to modify runner.sh to tune JVM settings.

::

  $ bin/runner.sh my_project.Boot

You may need to write the above command line to INSTALL file, for example, so
that the user of your project know how to start the web server.

To change the port of the web server, change ``http_port `` in ``config/xitrum.properties``.

Base URI
--------

Virtual host is ususally used when running behind a proxy server. But if you
don't want to use virtual host, you want to use base URI, see ``base_uri`` option
in the file ``config/xitrum.properties``.
