Filters
=======

Before filters
--------------

Before filters are run before an action is run.
They are funtions that take no argument and returns true or false.
If a before filter returns false, all filters after it and the action will
not be run.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("before_filter")
  class MyAction extends Action {
    beforeFilter {
      logger.info("I run therefore I am")
      true
    }

    // This method is run after the above filters
    def execute() {
      respondInlineView("Before filters should have been run, please check the log")
    }
  }

After filters
-------------

After filters are run after an action is run.
They are functions that take no argument. Their return value will be ignored.

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("after_filter")
  class MyAction extends Action {
    afterFilter {
      logger.info("Run at " + System.currentTimeMillis())
    }

    def execute() {
      respondText("After filter should have been run, please check the log")
    }
  }

Around filters
---------------

::

  import xitrum.Action
  import xitrum.annotation.GET

  @GET("around_filter")
  class MyAction extends Action {
    aroundFilter { action =>
      val begin = System.currentTimeMillis()
      action()
      val end   = System.currentTimeMillis()
      logger.info("The action took " + (end - begin) + " [ms]")
    }

    def execute() {
      respondText("Around filter should have been run, please check the log")
    }
  }

If there are many around filters, they will be nested.

Priority
--------

* Before filters are run first, then around filters, then after filters
* If one of the before filters returns false, the rest (including around and
  after filters) will not be run
* After filters are always run if at least an around filter is run
* If an around filter decide not to call ``action``, the inner nested
  around filters will not be run

::

  before1 -true-> before2 -true-> +--------------------+ --> after1 --> after2
                                  | around1 (1 of 2)   |
                                  |   around2 (1 of 2) |
                                  |     action         |
                                  |   around2 (2 of 2) |
                                  | around1 (2 of 2)   |
                                  +--------------------+
