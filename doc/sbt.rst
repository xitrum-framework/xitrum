Getting started
===============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

Xitrum is for Scala projects. Scala projects use SBT as the de facto build tool.
Please use SBT 0.10.0+:
https://github.com/harrah/xsbt

The discussion below assumes that you have installed SBT and had ``sbt`` command
in your PATH.

Netty 4
-------

File upload feature in Xitrum needs Netty 4, which has not been released. You
must download and build it yourself.

Download with git:

::

  $ git clone https://github.com/trustin/netty

Add to Netty's pom.xml:

::

  <repositories>
     <repository>
       <id>repository.jboss.org</id>
       <name>JBoss Releases Repository</name>
       <url>http://repository.jboss.org/maven2</url>
     </repository>
   </repositories>

   <pluginRepositories>
     <pluginRepository>
       <id>repository.jboss.org</id>
       <name>JBoss Releases Repository</name>
       <url>http://repository.jboss.org/maven2</url>
     </pluginRepository>
   </pluginRepositories>

Build with Maven:

::

  $ wget https://repository.jboss.org/nexus/content/repositories/releases/org/jboss/logging/jboss-logging-spi/2.1.2.GA/jboss-logging-spi-2.1.2.GA.jar
  $ mvn install:install-file -DgroupId=org.jboss.logging -DartifactId=jboss-logging-spi -Dpackaging=jar -Dversion=2.1.2.GA -Dfile=jboss-logging-spi-2.1.2.GA.jar -DgeneratePom=true
  $ MAVEN_OPTS=-Xmx512m mvn -Dmaven.test.skip=true install

Above is the quick and dirty way. For long way: https://issues.jboss.org/browse/NETTY-387

This section will be removed when Netty 4 is officially released.

Add Xitrum plugin to SBT
------------------------

Add to file ``~/.sbt/plugins/build.sbt`` (or create it):

::

  resolvers += "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

  libraryDependencies += "tv.cntt" %% "xitrum-plugin" % "1.1-SNAPSHOT"

Now you have 2 commands: ``sbt xitrum-new`` and :doc:`sbt xitrum-dist </deploy>`.

xitrum-new
----------

Create a new project:

::

  $ mkdir my_project
  $ cd my_project
  $ sbt xitrum-new

This skeleton will be created:

::

  config
    hazelcast.xml
    logback.xml
    xitrum.properties
  public
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

Run ``sbt run`` to run my_project.Boot, which starts the web server.
Then browse to ``http://localhost:8000/`` to see the web site.

In build.sbt, you will see that logback-classic is a dependency. It is because
Xitrum uses SLF4J. You must provide an SLF4J implentation.

Other common SBT tasks:

* ``sbt compile``: Compile .java and .scala files to ``target`` directory
* ``sbt package``: Package compiled .class files to a .jar file

Create Eclipse project
----------------------

If you use Eclipse, there is `Scala plugin for Eclipse <http://www.scala-ide.org/>`_.

To create .project file so that your project becomes an Eclipse project:

1. Add to ``~/.sbt/plugins/build.sbt`` the contents as described at https://github.com/typesafehub/sbteclipse
2. At your SBT project directory, run ``sbt eclipse``

See `SBT plugins list <https://github.com/harrah/xsbt/wiki/sbt-0.10-plugins-list>`_
for plugins for other IDEs.

JRebel
------

In development mode, you start the web server with ``sbt run``. Normally, when
you change your source code, you need to rerun ``sbt run`` again and again.
With `JRebel <http://www.zeroturnaround.com/jrebel/>`_ you can avoid that.

To setup JRebel with SBT:

1. Download JRebel
2. Apply for a `free license for Scala <http://sales.zeroturnaround.com/>`_
3. Save ``jrebel.lic`` (the free license above) to the same directory with ``jrebel.jar``
4. Add ``-noverify -javaagent:/path/to/jrebel/jrebel.jar`` to the ``sbt`` command line

Now:

1. Run ``sbt run`` in one console
2. Run ``sbt ~compile`` in another console to compile in continuous/incremental mode

If you use IDE like Eclipse with Scala plugin, you don't need to run ``sbt ~compile``,
just save the source code, and the IDE will automatically compile for you, and
the ``sbt run`` process will automatically load the new source code, thanks to
JRebel.
