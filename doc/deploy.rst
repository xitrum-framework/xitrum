Distribution directory layout
=============================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s8.jpg

Please see the SBT section.

Using the the dist task as in the example, after running ``sbt dist`` you will have
"dist" directory inside "target" directory. Its structure is typically:

::

  bin
    runner.sh               <-- Script to start the web server
  config                    <-- Config files are outside .jar files, edit any time you want
    my_project.properties             They are put in classpath by runner.sh
    logback.properties
    xitrum.properties
    i18n
      en.po
      ja.po
      mylib
        en.po
        ja.po
  lib
    blog.jar                <-- css/img/js are concrete thus packaged here
    ...
  public
    upload                  <-- Directory to store user uploaded files
  README                        It may be a symlink

You run by:

::

  bin/runner.sh my.project.boot.Klass
