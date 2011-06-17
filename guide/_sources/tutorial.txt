Tutorial
========

This chapter describes how to create and run a Xitrum project.
**It assumes that you are using Linux and you have installed Java.**

Create a new empty Xitrum project
---------------------------------

To create a new empty project, download
`xitrum-new.zip <https://github.com/ngocdaothanh/xitrum-new/archive/master.zip>`_:

::

  wget -O xitrum-new.zip https://github.com/ngocdaothanh/xitrum-new/archive/master.zip

Or:

::

  curl -L -o xitrum-new.zip https://github.com/ngocdaothanh/xitrum-new/archive/master.zip

Run
---

The de facto stardard way of building Scala projects is using
`SBT <https://github.com/harrah/xsbt/wiki/Setup>`_. The newly created project
has already included SBT 0.11.3-2 in ``sbt`` directory. If you want to install
SBT yourself, see its `setup guide <https://github.com/harrah/xsbt/wiki/Setup>`_.

Change to the newly created project directory and run ``sbt/sbt run``:

::

  unzip xitrum-new.zip
  cd xitrum-new
  sbt/sbt run

This command will download all :doc:`dependencies </deps>`, compile the project,
and run the class ``quickstart.Boot``, which starts the web server. In the console,
you will see all the routes:

::

  [INFO] Routes:
  GET  /                  quickstart.action.SiteIndex
  GET  /xitrum/routes.js  xitrum.routing.JSRoutesAction
  [INFO] HTTP server started on port 8000
  [INFO] HTTPS server started on port 4430
  [INFO] Xitrum started in development mode

On startup, all routes will be collected and output to log. It is very
convenient for you to have a list of routes if you want to write documentation
for 3rd parties about the RESTful APIs in your web application.

Open http://localhost:8000/ or https://localhost:4430/ in your browser. In the
console you will see request information:

::

  [DEBUG] GET quickstart.action.SiteIndex, 1 [ms]
