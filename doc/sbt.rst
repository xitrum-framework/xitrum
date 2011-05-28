SBT
===

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

Please see:
http://code.google.com/p/simple-build-tool/

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
