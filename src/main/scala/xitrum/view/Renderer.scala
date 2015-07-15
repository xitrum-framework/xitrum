package xitrum.view

import java.io.File
import xitrum.{Action, Config}

trait Renderer extends GetActionClassDefaultsToCurrentAction {
  this: Action =>

  var renderedView: Any = null

  def layout = renderedView

  //----------------------------------------------------------------------------

  def renderEventSource(data: Any, event: String = "message"): String = {
    val builder = new StringBuilder

    if (event != "message") {
      builder.append("event: ")
      builder.append(event)
      builder.append("\n")
    }

    val lines = data.toString.split("\n")
    val n = lines.length
    for (i <- 0 until n) {
      if (i < n - 1) {
        builder.append("data: ")
        builder.append(lines(i))
        builder.append("\n")
      } else {
        builder.append("data: ")
        builder.append(lines(i))
      }
    }

    builder.append("\r\n\r\n")
    builder.toString
  }

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the location to "renderedView",
   * then calls the layout function.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, location: String, options: Map[String, Any]): String = {
    Config.xitrum.template match {
      case Some(engine) =>
        renderedView = engine.renderView(location, this, options)
        customLayout.apply().toString

      case None =>
        log.warn("No template engine is configured")
        ""
    }
  }

  /**
   * Renders the template associated with the location to "renderedView",
   * then calls the layout function.
   */
  def renderView(customLayout: () => Any, location: String): String = {
    renderView(customLayout, location, Map.empty[String, Any])
  }

  def renderView(location: String, options: Map[String, Any]): String =
    renderView(layout _, location, options)

  def renderView(location: String): String =
    renderView(layout _, location, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the location to "renderedView",
   * then calls the layout function.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, location: Class[_ <: Action], options: Map[String, Any]): String = {
    renderView(customLayout, templatePathFromClass(location), options)
  }

  def renderView[T <: Action: Manifest](customLayout: () => Any, options: Map[String, Any]): String =
    renderView(customLayout, getActionClass[T], options)

  def renderView[T <: Action: Manifest](customLayout: () => Any): String =
    renderView(customLayout, getActionClass[T], Map.empty[String, Any])

  def renderView[T <: Action: Manifest](options: Map[String, Any]): String =
    renderView(layout _, getActionClass[T], options)

  def renderView[T <: Action: Manifest](): String =
    renderView(layout _, getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def renderViewNoLayout(location: String, options: Map[String, Any]): String =
    Config.xitrum.template match {
      case Some(engine) =>
        val ret = engine.renderView(location, this, options)
        renderedView = ret
        ret

      case None =>
        log.warn("No template engine is configured")
        ""
    }

  def renderViewNoLayout(location: String): String =
    renderViewNoLayout(location, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def renderViewNoLayout(location: Class[_ <: Action], options: Map[String, Any]): String =
    renderViewNoLayout(templatePathFromClass(location), options)

  def renderViewNoLayout[T <: Action: Manifest](options: Map[String, Any]): String =
    renderViewNoLayout(getActionClass[T], options)

  def renderViewNoLayout[T <: Action: Manifest](): String =
    renderViewNoLayout(getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def renderFragment(directory: String, fragment: String, options: Map[String, Any]): String =
    Config.xitrum.template match {
      case Some(engine) =>
        engine.renderFragment(directory, fragment, this, options)

      case None =>
        log.warn("No template engine is configured")
        ""
    }

  def renderFragment(directory: String, fragment: String): String =
    renderFragment(directory, fragment, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def renderFragment(directory: Class[_ <: Action], fragment: String, options: Map[String, Any]): String =
    renderFragment(fragmentDirectoryFromClass(directory), fragment, options)

  def renderFragment[T <: Action: Manifest](fragment: String, options: Map[String, Any]): String =
    renderFragment(getActionClass[T], fragment, options)

  def renderFragment[T <: Action: Manifest](fragment: String): String =
    renderFragment(getActionClass[T], fragment, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString
  }

  //----------------------------------------------------------------------------

  /** Converts, for example, a.b.C to a/b/C. */
  private def templatePathFromClass(klass: Class[_]): String = {
    klass.getName.replace('.', File.separatorChar)
  }

  /** Converts, for example, a.b.C to a/b/C. */
  private def fragmentDirectoryFromClass(klass: Class[_]): String = {
    // location.getPackage will only return a non-null value if the current
    // ClassLoader is already aware of the package
    klass.getName.split('.').dropRight(1).mkString(File.separator)
  }
}
