Filters
=======

Before filters
--------------

Before filters are run before an action is run.
They are funtions that take no argument and returns true or false.
If a before filter returns false, all filters after it and the action will
not be run.

::

  import xitrum.Controller

  class MyController extends Action {
    beforeFilter {
      logger.info("I run therefore I am")
      true
    }

    // This method is run after the above filters
    val index =   @GET("before_filter") {
      respondInlineText("Before filters should have been run, please check the log")
    }
  }

Before filters can be skipped using ``skipBeforeFilter``.

::

  import xitrum.Controller

  class AppController extends Controller {
    val authenticate = beforeFilter {
      basicAuthenticate("Realm") { (username, password) =>
        username == "foo" && password == "bar"
      }
    }
  }

  // This controller is protected by authentication
  class AController extends AppController {
    val index = GET("secretplace") {
      respondText("secretplace")
    }
  }

  // This is not
  class AnotherController extends AppController {
    skipBeforeFilter(authenticate)

    val index = GET("nothingspecial") {
      respondText("nothingspecial")
    }
  }

After filters
-------------

After filters are run after an action is run.
They are functions that take no argument. Their return value will be ignored.

::

  import xitrum.Controller

  class MyController extends Controller {
    val index = GET("after_filter") {
      respondText("After filter should have been run, please check the log")
    }

    afterFilter {
      logger.info("Run at " + System.currentTimeMillis())
    }
  }

After filters can be skipped using ``skipAfterFilter``.

Around filters
---------------

::

  import xitrum.Controller

  class MyController extends Controller {
    aroundFilter { action =>
      val begin = System.currentTimeMillis()
      action()
      val end   = System.currentTimeMillis()
      logger.info("The action took " + (end - begin) + " [ms]")
    }

    val index = GET("around_filter") {
      respondText("Around filter should have been run, please check the log")
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
* If an around filter decide not to call ``action``, the inner nested
  around filters will not be run

::

  before1 -true-> before2 -true-> +----------------------+ --> after1 --> after2
                                  | around1 (1 of 2)     |
                                  | +------------------+ |
                                  | | around2 (1 of 2) | |
                                  | | action           | |
                                  | | around2 (2 of 2) | |
                                  | +------------------+ |
                                  | around2 (2 of 2)     |
                                  +----------------------+
