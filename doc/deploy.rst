Prepare for distribution
========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

Please see the :doc:`SBT section </sbt>`.

xitrum-dist
-----------

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

  $ bin/runner.sh my_package.BootClass

You may need to write the above command line to INSTALL file, for example, so
that the user of your project know how to start the web server.

Copy additional files to target/dist
------------------------------------

``sbt xitrum-dist`` command line simply copies ``config`` and ``public``
directories to ``target/dist``. If you want it to copy additional files and
directories (README, INSTALL, doc etc.)

TODO
