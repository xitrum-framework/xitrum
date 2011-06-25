Tutorial
========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

This section describes how to create and run a simple Xitrum project.
It assumes that you are using Linux and have installed Java.

Install SBT
-----------

Scala projects use `SBT <https://github.com/harrah/xsbt>`_ as the de facto build tool.
Xitrum needs SBT 0.10.0+.

Follow `instructions <https://github.com/harrah/xsbt/wiki/Setup>`_ to install.

Netty 4
-------

**This part will be removed when Netty 4 is officially released.**

File upload feature in Xitrum needs `Netty <https://github.com/netty/netty>`_ 4,
which has not been released. You must download and build it yourself.

`Download <https://github.com/netty/netty/archives/master>`_ it.

Add to its pom.xml:

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

Build with `Maven <http://maven.apache.org/>`_:

::

  $ wget http://bit.ly/jebzNn
  $ mvn install:install-file -DgroupId=org.jboss.logging -DartifactId=jboss-logging-spi \
    -Dpackaging=jar -Dversion=2.1.2.GA -Dfile=jboss-logging-spi-2.1.2.GA.jar -DgeneratePom=true
  $ MAVEN_OPTS=-Xmx512m mvn -Dmaven.test.skip=true install

Above is the quick and dirty way. For long way: https://issues.jboss.org/browse/NETTY-387.

Add Xitrum plugin to SBT
------------------------

Create file ``~/.sbt/plugins/build.sbt``, then add to it:

::

  resolvers += "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

  libraryDependencies += "tv.cntt" %% "xitrum-plugin" % "1.1-SNAPSHOT"

Now you have 2 commands: ``sbt xitrum-new`` and :doc:`sbt xitrum-package </deploy>`.

Create a new Xitrum project
---------------------------

::

  $ cd /tmp
  $ mkdir my_project
  $ cd my_project
  $ sbt xitrum-new

A new project skeleton will be created:

::

  my_project
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

In build.sbt, you see that logback-classic is a dependency. It is because
Xitrum uses SLF4J, so the project must provide an SLF4J implentation, e.g. logback-classic.

Run
---

Run ``sbt run``. This command will download dependencies, compile the project,
and run the class my_project.Boot, which starts the web server.

Open http://localhost:8000/ in your browser.
