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
        distTask,
        unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }
      )
    )

    // Task "dist" ---------------------------------------------------------------

    val dist = TaskKey[Unit]("dist", "Prepare target/dist directory ready for production distribution")

    lazy val distTask = dist <<= (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map { (libs, baseDir, target, scalaVersion) =>
      val distDir = new File(target,  "dist")

      // Copy bin directory
      val binDir1 = new File(baseDir, "bin")
      val binDir2 = new File(distDir, "bin")
      IO.copyDirectory(binDir1, binDir2)
      for (file <- binDir2.listFiles)
        if (file.isFile && file.name.endsWith("sh"))
          file.setExecutable(true)

      // Copy config directory
      val configDir1 = new File(baseDir, "config")
      val configDir2 = new File(distDir, "config")
      IO.copyDirectory(configDir1, configDir2)

      // Copy public directory
      val publicDir1 = new File(baseDir, "public")
      val publicDir2 = new File(distDir, "public")
      IO.copyDirectory(publicDir1, publicDir2)

      // Copy lib directory
      val libDir = new File(distDir, "lib")

      // Copy dependencies
      libs.foreach { lib => IO.copyFile(lib.data, new File(libDir + "/%s".format(lib.data.getName))) }

      // Copy .jar files are created after running "sbt package"
      val jarDir = new File(target, "scala-" + scalaVersion.replace('-', '.'))
      for (file <- jarDir.listFiles)
        if (file.isFile && file.name.endsWith("jar"))
          IO.copyFile(file, new File(libDir + "/%s".format(file.getName)))
    }
  }

With the above, you can:

* Run `sbt run` to run `my.project.boot.Klass`
* Run `sbt dist` to prepare target/dist directory ready for production distribution

You may want to modify dist task above to suit your project.

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
