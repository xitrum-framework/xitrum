Prepare for distribution
========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

Xitrum is designed in mind to run in production environment as multiple instances
behind a proxy server or load balancer:

::

                                / Xitrum instance 1
  Load balancer/proxy server ---- Xitrum instance 2
                                \ Xitrum instance 3

Prepare directory for distribution
----------------------------------

Please see the :doc:`SBT section </sbt>` about ``xitrum-dist``.

Run ``sbt xitrum-dist`` to prepare ``target/dist`` directory, ready for production distribution:

::

  target/dist
    bin
      runner.sh
    config
      [config files]
    public
      [web static files]
    lib
      [dependencies and packaged project file]

runner.sh is the script to start the web server in production environment.
You may want to modify runner.sh to tune JVM settings.

::

  $ bin/runner.sh my_project.Boot

You may need to write the above command line to INSTALL file, for example, so
that the user of your project know how to start the web server.

Config to copy additional files to target/dist
----------------------------------------------

``sbt xitrum-dist`` command line simply copies ``config`` and ``public``
directories to ``target/dist``. If you want it to copy additional files and
directories (README, INSTALL, doc etc.)

TODO
