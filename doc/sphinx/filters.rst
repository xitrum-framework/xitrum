Filters
=======

Before filters
--------------

Before filters are run before ``execute`` or ``postback`` method is run.
They are funtions that take no argument and returns true or false.
If a before filter returns false, all filters after it and ``execute`` will
not be run.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class MyAction extends Action {
    val myFilter = { () =>
      println("Run at " + System.currentTimeMillis)
      true
    }

    // Add this filter
    beforeFilter(myFilter)

    // Filter can be anonymous
    // This filter is run after the above
    beforeFilter { () =>
      println("I run therefore I am")
      true
    }

    // This method is run after the above filters
    override def execute {
      renderText("Hi")
    }
  }

Before filters can be skipped using ``skipBeforeFilter``.

::

  import xitrum.Action
  import xitrum.annotation.GET

  class AppAction extends Action {
    val authenticate = basicAuthenticate("Realm") { (username, password) =>
      username == "foo" && password == "bar"
    }

    beforeFilter(authenticate)
  }

  // This action is protected by authentication
  @GET("/secretplace")
  class AnAction extends AppAction {
    override def execute {
      renderText("secretplace")
    }
  }

  // This is not
  @GET("/nothingspecial")
  class AnotherAction extends AppAction {
    skipBeforeFilter(authenticate)

    override def execute {
      renderText("nothingspecial")
    }
  }

After filters
-------------

After filters are run after ``execute`` or ``postback`` method is run.
They are functions that take no argument. Their return value will be ignored.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class MyAction extends Action {
    override def execute {
      renderText("Hello")
    }

    afterFilter { () =>
      println(" World")
    }
  }

After filters can be skipped using ``skipAfterFilter``.

Around filters
---------------

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("/")
  class MyAction extends Action {
    aroundFilter { executeOrPostback =>
      val begin = System.currentTimeMillis
      executeOrPostback
      val end   = System.currentTimeMillis
      println("The action takes " + (end - begin) + " [ms]")
    }

    override def execute {
      renderText("Hi")
    }
  }

If there are many around filters, they will be nested.
Around filters can be skipped using ``skipAroundFilter``.

Priority
--------

* Before filters are run first, then around filters, then after filters
* If one of the before filters returns false, the rest (including around and
  after filters) will not be run
* After filters are always run if at least an around filter is run
* If an around filter decide not to call ``executeOrPostback``, the inner nested
  around filters will not be run

::

  before1 -true-> before2 -true-> +-----------------------+ --> after1 --> after2
                                  | around1 (1 of 2)      |
                                  | +-------------------+ |
                                  | | around2 (1 of 2)  | |
                                  | | executeOrPostback | |
                                  | | around2 (2 of 2)  | |
                                  | +-------------------+ |
                                  | around2 (2 of 2)      |
                                  +-----------------------+
