Deploy to production server
===========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

Xitrum is designed in mind to run in production environment as multiple instances
behind a proxy server or load balancer:

::

                                / Xitrum instance 1
  Load balancer/proxy server ---- Xitrum instance 2
                                \ Xitrum instance 3

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
    public
      [static files, accessible from browsers]
    lib
      [dependencies and packaged project file]

runner.sh is the script to start the web server in production environment.
You may want to modify runner.sh to tune JVM settings.

::

  $ bin/runner.sh my_project.Boot

You may need to write the above command line to INSTALL file, for example, so
that the user of your project know how to start the web server.

Copy additional files to target/xitrum_package
----------------------------------------------

``sbt xitrum-package`` command simply copies ``config`` and ``public``
directories to ``target/xitrum_package``. If you want it to copy additional files
and directories (README, INSTALL, doc etc.), config ``build.sbt`` like this:

::

  TODO

Now run ``sbt xitrum-package`` and check ``target/xitrum_package`` directory.
