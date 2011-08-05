Tutorial
========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

This chapter describes how to create and run a Xitrum project.
**It assumes that you are using Linux and you have installed Java.**

Install SBT
-----------

Most Scala projects use `SBT <https://github.com/harrah/xsbt>`_ as the de facto build tool.
Xitrum needs SBT 0.10.1+.

Follow `instructions <https://github.com/harrah/xsbt/wiki/Setup>`_ to install.
You can do like this:

::

  $ mkdir -p ~/opt/sbt
  $ cd ~/opt/sbt
  $ wget http://typesafe.artifactoryonline.com/typesafe/ivy-releases/org.scala-tools.sbt/sbt-launch/0.10.1/sbt-launch.jar
  $ echo 'java -Xmx512m -XX:MaxPermSize=128m -Dsbt.boot.directory="$HOME/.sbt/boot" -jar `dirname $0`/sbt-launch.jar "$@"' > sbt
  $ chmod +x sbt
  $ export PATH=$PATH:~/opt/sbt

You should add the line ``export PATH=$PATH:~/opt/sbt`` to ``~/.profile``.

Add Xitrum plugin to SBT
------------------------

Create file ``~/.sbt/plugins/build.sbt``, then add to it (there must be a blank
line between the 2 lines below):

::

  resolvers += "Scala Tools Snapshot Repository" at "http://nexus.scala-tools.org/content/repositories/snapshots"

  libraryDependencies += "tv.cntt" %% "xitrum-plugin" % "1.1-SNAPSHOT"

You can do like this:

::

  $ mkdir -p ~/.sbt/plugins
  $ echo 'resolvers += "Scala Tools Snapshot Repository" at "http://nexus.scala-tools.org/content/repositories/snapshots"' >> ~/.sbt/plugins/build.sbt
  $ echo -e "\n" >> ~/.sbt/plugins/build.sbt
  $ echo 'libraryDependencies += "tv.cntt" %% "xitrum-plugin" % "1.1-SNAPSHOT"' >> ~/.sbt/plugins/build.sbt

Now you have 2 commands: ``sbt xitrum-new`` and :doc:`sbt xitrum-package </deploy>`.

Create a new Xitrum project
---------------------------

Use ``sbt xitrum-new`` to create a new Xitrum project:

::

  $ cd /tmp
  $ mkdir my_project
  $ cd my_project
  $ sbt xitrum-new

A new project skeleton will be created:

::

  my_project
    config
      hazelcast_cluster_member_or_super_client.xml
      hazelcast_java_client.properties
      logback.xml
      xitrum.properties
    static
      public
        css
          960
            reset.css
            text.css
            960.css
          app.css
      404.html
      500.html
      favicon.ico
      robots.txt
    src
      main
        scala
          my_project
            Boot.scala
            action
              AppAction.scala
              IndexAction.scala
    build.sbt

Run
---

Run ``sbt run``. This command will download dependencies, compile the project,
and run the class ``my_project.Boot``, which starts the web server.

Open http://localhost:8000/ in your browser.

In the console, you will see all the routes and request information:

::

  [INFO] x.r.Routes$: Routes:
  GET / my_project.action.IndexAction

  [INFO] x.Server: Xitrum started on port 8000 in development mode
  [DEBUG] x.h.u.Dispatcher$: GET my_project.action.IndexAction, 1 [ms]

On startup, all routes will be collected and output to log. It is very
convenient for you to have a list of routes if you want to write documentation
for 3rd parties about the RESTful APIs in your web application.
