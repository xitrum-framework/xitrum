SBT
===

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

Please use SBT 0.7.7:
http://code.google.com/p/simple-build-tool/

SBT 0.10.0+ does not work well yet:
https://github.com/harrah/xsbt

This section discusses your web application structure when you use SBT.

SBT project source directory layout
-----------------------------------

A typical blog application will have this directory layout:

::

  config
    blog.properties
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
    build.properties
    build
      Project.scala
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

SBT project dependency
----------------------

To add Xitrum to project dependencies:

::

  "tv.cntt" %% "xitrum" % "1.0-SNAPSHOT"

Xitrum uses SLF4J 1.6.1. You must provide an SLF4J implentation that compatible
with SLF4J 1.6.1, like logback-classic 0.9.27:

::

  "ch.qos.logback" % "logback-classic" % "0.9.27"


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
