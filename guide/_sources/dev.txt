Development flow with SBT, Eclipse/IntelliJ IDEA, and JRebel
============================================================

Import the project to Eclipse
-----------------------------

You can `use Eclipse to write Scala code <http://scala-ide.org/>`_.

From the project directory, run:

::

  sbt/sbt eclipse

``.project`` file for Eclipse will be created from definitions in ``build.sbt``.
Now open Eclipse, and import the project.

Import the project to IntelliJ IDEA
-----------------------------------

You can also use `IntelliJ IDEA <http://www.jetbrains.com/idea/>`_, which also
has very good support for Scala.

To generate project files for IDEA, run:

::

  sbt/sbt gen-idea

Ignore files
------------

Create a new project as described at the :doc:`tutorial </tutorial>`.
These should be `ignored <https://github.com/ngocdaothanh/xitrum-new/blob/master/.gitignore>`_:

::

  .*
  log
  project/project
  project/target
  routes.cache
  target

Install JRebel
--------------

In development mode, you start the web server with ``sbt/sbt run``. Normally, when
you change your source code, you have to press CTRL+C to stop, then run ``sbt/sbt run``
again. This may take tens of seconds everytime.

With `JRebel <http://www.zeroturnaround.com/jrebel/>`_ you can avoid that. JRebel
provides free license for Scala developers!

Install:

1. Apply for a `free license for Scala <http://sales.zeroturnaround.com/>`_
2. Download and install JRebel using the license above
3. Add ``-noverify -javaagent:/path/to/jrebel/jrebel.jar`` to the ``sbt/sbt`` command line

Example:

::

  java -noverify -javaagent:"$HOME/opt/jrebel/jrebel.jar" \
       -Xmx1024m -XX:MaxPermSize=128m -Dsbt.boot.directory="$HOME/.sbt/boot" \
       -jar `dirname $0`/sbt-launch.jar "$@"

Use JRebel
----------

1. Run ``sbt/sbt run``
2. In Eclipse, try editing a Scala file, then save it

The Scala plugin for Eclipse will automatically recompile the file. And JRebel will
automatically reload the generated .class files.

If you use a plain text editor, not Eclipse:

1. Run ``sbt/sbt run``
2. Run ``sbt/sbt ~compile`` in another console to compile in continuous/incremental mode
3. In the editor, try editing a Scala file, and save

The ``sbt/sbt ~compile`` process will automatically recompile the file, and JRebel will
automatically reload the generated .class files.

``sbt/sbt ~compile`` works fine in bash and sh shell. In zsh shell, you need to use
``sbt/sbt "~compile"``, or it will complain "no such user or named directory: compile".

Currently routes are not reloaded, even in development mode with JRebel.

To use JRebel in Eclipse, see `this tutorial <http://zeroturnaround.com/software/jrebel/eclipse-jrebel-tutorial/>`_.
