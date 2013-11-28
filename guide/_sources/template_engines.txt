Template engines
================

The configured template engine will be called when :doc:`renderView, renderFragment,
or respondView </action_view>` is called.

Config template engine
----------------------

In `config/xitrum.conf <https://github.com/ngocdaothanh/xitrum-new/blob/master/config/xitrum.conf>`_,
template engine can be configured in one of the following 2 forms, depending on the engine you use:

::

  template = my.template.EngineClassName

Or:

::

  template {
    "my.template.EngineClassName" {
      option1 = value1
      option2 = value2
    }
  }

The default template engine is `xitrum-scalate <https://github.com/ngocdaothanh/xitrum-scalate>`_.

Remove template engine
----------------------

If you create only RESTful APIs in your project, normally you don't call
renderView, renderFragment, or respondView. In this case, you can even remove
template engine from your project to make it lighter. Just remove or comment out
the ``templateEngine`` in config/xitrum.conf.

Then remove template related configs from your project.

Create your own template engine
-------------------------------

To create your own template engine, create a class that implements
`xitrum.view.TemplateEngine <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/view/TemplateEngine.scala>`_.
Then set your class in config/xitrum.conf.

For an example, see `xitrum-scalate <https://github.com/ngocdaothanh/xitrum-scalate>`_.
