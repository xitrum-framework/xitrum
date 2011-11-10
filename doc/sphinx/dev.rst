Development flow with SBT, Eclipse, and JRebel
==============================================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s12.jpg

This chapter assumes that you have installed Eclipse and
`Scala plugin for Eclipse <http://www.scala-ide.org/>`_.

Install plugins for SBT
-----------------------

Install Xitrum plugin as in the :doc:`tutorial </tutorial>`.

Install Eclipse plugin by adding to file ``~/.sbt/plugins/build.sbt``
the content as described at https://github.com/typesafehub/sbteclipse.

Create a new Xitrum project
---------------------------

::

  $ cd <Eclipse workspace directory>
  $ mkdir my_project
  $ cd my_project
  $ sbt xitrum-new
  $ sbt eclipse

Now open Eclipse, and import the created project.

Install JRebel
--------------

In development mode, you start the web server with ``sbt run``. Normally, when
you change your source code, you have to press CTRL+C to stop, then run ``sbt run``
again. This may take tens of seconds everytime.

With `JRebel <http://www.zeroturnaround.com/jrebel/>`_ you can avoid that. JRebel
provides free license for Scala developers!

Install:

1. Apply for a `free license for Scala <http://sales.zeroturnaround.com/>`_
2. Download and install JRebel using the license above
3. Add ``-noverify -javaagent:/path/to/jrebel/jrebel.jar`` to the ``sbt`` command line

Example:

::

  java -noverify -javaagent:"$HOME/opt/jrebel/jrebel.jar" \
       -Xmx1024m -XX:MaxPermSize=128m -Dsbt.boot.directory="$HOME/.sbt/boot" \
       -jar `dirname $0`/sbt-launch.jar "$@"

Use JRebel
----------

1. Run ``sbt run``
2. In Eclipse, try editing a Scala file, and save

The Scala plugin for Eclipse will automatically recompile the file. And JRebel will
automatically reload the generated .class files.

If you use a plain text editor, not Eclipse:

1. Run ``sbt run``
2. Run ``sbt ~compile`` in another console to compile in continuous/incremental mode
3. In the editor, try editing a Scala file, and save

The ``sbt ~compile`` process will automatically recompile the file, and JRebel will
automatically reload the generated .class files.

``sbt ~compile`` works fine in bash and sh shell. In zsh shell, you need to use
``sbt "~compile"``, or it will complain "no such user or named directory: compile".

Routes are not reloaded
~~~~~~~~~~~~~~~~~~~~~~~

Routes like ``@GET("/")`` are not reloaded when you change them. Routes are only
scanned when the web server starts. It it because it takes several seconds to
rescan all routes.
