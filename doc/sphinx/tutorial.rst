Tutorial
========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s13.jpg

This chapter describes how to create and run a Xitrum project.
**It assumes that you are using Linux and you have installed Java.**

Create a new Xitrum project
---------------------------

Clone the xitrum-quickstart project:

::

  $ git clone https://github.com/ngocdaothanh/xitrum-quickstart.git

The de facto stardard way of building Scala projects is using
`SBT <https://github.com/harrah/xsbt/wiki/Setup>`_. The quickstart project
has already included SBT 0.11.2 in ``sbt`` directory. If you want to install
SBT yourself, see its `setup guide <https://github.com/harrah/xsbt/wiki/Setup>`_.

Run
---

Change to the newly created project directory and run ``sbt/sbt run``. This command
will download all dependencies, compile the project, and run the class
``quickstart.Boot``, which starts the web server.

Open http://localhost:8000/ or https://localhost:4430/ in your browser.

In the console, you will see all the routes and request information:

::

  [INFO] Routes:
  GET / quickstart.action.IndexAction

  [INFO] HTTP server started on port 8364
  [INFO] HTTPS server started on port 4364
  [INFO] Xitrum started in development mode
  [DEBUG] GET quickstart.action.IndexAction, 1 [ms]

On startup, all routes will be collected and output to log. It is very
convenient for you to have a list of routes if you want to write documentation
for 3rd parties about the RESTful APIs in your web application.

For better startup speed, routes are cached to file ``routes.sclasner``.
While developing, routes in .class files in the ``target`` directory are not
cached. If you change library dependencies that contain routes, you may need to
delete ``routes.sclasner``. This file should not be committed to your project
source code repository.
