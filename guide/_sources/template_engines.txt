Template engines
================

The configured template engine will be called when renderView, renderFragment,
or respondView is called.

Config template engine
----------------------

The default template engine is `xitrum-scalate <https://github.com/ngocdaothanh/xitrum-scalate>`_.

config/xitrum.conf:

::

  templateEngine = xitrum.view.Scalate

You can change ``templateEngine`` to another one. Note that each template engine
may require its own config. In the case of Scalate:

project/plugins.sbt:

::

  // For compiling Scalate templates in the compile phase of SBT
  addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

build.sbt:

::

  // Import xsbt-scalate-generator keys; this must be at top of build.sbt, or SBT will complain
  import ScalateKeys._

  ...
  ...

  libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "1.2"

  // Precompile Scalate
  seq(scalateSettings:_*)

  scalateTemplateConfig in Compile := Seq(TemplateConfig(
    file("src") / "main" / "scalate",  // See config/scalate.conf
    Seq(),
    Seq(Binding("helper", "xitrum.Action", true))
  ))

config/scalate.conf (included in application.conf):

::

  scalate {
    defaultType = jade              # jade, mustache, scaml, or ssp
    dir         = src/main/scalate  # Only used in development mode
  }

Remove template engine
----------------------

If you create only RESTful APIs in your project, normally you don't call
renderView, renderFragment, or respondView. In this case, you can even remove
template engine from your project to make it lighter. Just comment out like this:

config/xitrum.conf:

::

  #templateEngine = xitrum.view.Scalate

Then remove template related configs from your project.

Create your own template engine
-------------------------------

To create your own template engine, create a class that implements
`xitrum.view.TemplateEngine <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/view/TemplateEngine.scala>`_.
Then set your class in config/xitrum.conf.

For an example, see `xitrum-scalate <https://github.com/ngocdaothanh/xitrum-scalate>`_.
