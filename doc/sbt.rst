SBT
===

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

Please use SBT 0.10.0+:
https://github.com/harrah/xsbt

This section discusses your web application structure when you use SBT.

SBT project source directory layout
-----------------------------------

A typical blog application will have this directory layout:

::

  config
    my_project.properties
    logback.properties
    xitrum.properties
    i18n
      en.po
      ja.po
      mylib
        en.po
        ja.po
  lib
    jrebel.jar
  lib_managed
  project
    Build.scala
  public
    upload
  src
    main
      scala
        blog
          action
            ArticleIndex.scala
            ArticleShow.scala
            ArticleNewEdit.scala
          helper
            ArticleHelper.scala
          model
            Article.scala
      resources
        public
          blog
            css
            img
            js
  README

Build.scala example
-------------------

Xitrum uses SLF4J 1.6.1. You must provide an SLF4J implentation that compatible
with SLF4J 1.6.1, like logback-classic 0.9.28. So basically, you need these 2
dependencies:

::

  "tv.cntt"        %% "xitrum"          % "1.1-SNAPSHOT"

and

::

  "ch.qos.logback" %  "logback-classic" % "0.9.28"

A full example of Build.scala:

::

  import sbt._
  import Keys._

  object MyBuild extends Build {
    val mySettings = Defaults.defaultSettings ++ Seq(
      organization := "my.organization",
      name         := "project_name",
      version      := "1.0-SNAPSHOT",
      scalaVersion := "2.9.0-1"
    )

    val myResolvers = Seq(
      // For Xitrum
      "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots",

      // For Netty 4, remove this when Netty 4 is released
      "Local Maven Repository"       at "file://" + Path.userHome.absolutePath + "/.m2/repository"
    )

    val myLibraryDependencies = Seq(
      "tv.cntt"        %% "xitrum"          % "1.1-SNAPSHOT",
      "ch.qos.logback" %  "logback-classic" % "0.9.28"
    )

    lazy val project = Project (
      "project",
      file ("."),
      settings = mySettings ++ Seq(
        resolvers           := myResolvers,
        libraryDependencies := myLibraryDependencies,

        mainClass           := Some("my.project.boot.Klass"),
        unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }
      )
    )
  }

With the above, you can run these tasks:

* ``sbt update``: Download dependencies
* ``sbt compile``: Compile .java and .scala files to ``target`` directory
* ``sbt run``: Run ``my.project.boot.Klass``
* ``sbt package``: Package the project to a .jar file

You may want to modify dist task above to suit your project.

Create Eclipse project
----------------------

To create .project file so that your project becomes an Eclipse project:

1. Create directory ``~/.sbt/plugins``
2. Inside that directory, create file build.sbt, with the contents as described at https://github.com/typesafehub/sbteclipse
3. At your SBT project directory, run ``sbt eclipse``

xitrum-dist
-----------

Add to ``~/.sbt/plugins/build.sbt``:

::

  // "xitrum-plugin" task
  resolvers += "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

  libraryDependencies += "tv.cntt" %% "xitrum-plugin" % "1.1-SNAPSHOT"

Now in all SBT projects, you can run ``sbt xitrum-dist`` to
prepare ``target/dist`` directory, ready for production distribution.
This task will copy to ``target/dist``:

* bin
* config
* public
* dependencies and packaged project file(s) (.jar files)

Netty 4
-------

File upload feature in Xitrum needs Netty 4, which has not been released. You
must download and build it yourself.

Download with git:

::

  git clone https://github.com/trustin/netty

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

  wget https://repository.jboss.org/nexus/content/repositories/releases/org/jboss/logging/jboss-logging-spi/2.1.2.GA/jboss-logging-spi-2.1.2.GA.jar
  mvn install:install-file -DgroupId=org.jboss.logging -DartifactId=jboss-logging-spi -Dpackaging=jar -Dversion=2.1.2.GA -Dfile=jboss-logging-spi-2.1.2.GA.jar -DgeneratePom=true
  MAVEN_OPTS=-Xmx512m mvn -Dmaven.test.skip=true install

Above is the quick and dirty way. For long way: https://issues.jboss.org/browse/NETTY-387
