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
   * Renders the template associated with an action to "renderedTemplate",
   * then calls the layout function.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, location: Class[_ <: Action], options: Map[String, Any]): String = {
    Config.xitrum.templateEngine match {
      case Some(engine) =>
        renderedView = engine.renderView(location, this, options)
        customLayout.apply().toString

      case None =>
        log.warn("No template engine is configured")
        ""
    }
  }

  def renderView[T <: Action : Manifest](customLayout: () => Any, options: Map[String, Any]): String =
    renderView(customLayout, getActionClass[T], options)

  def renderView[T <: Action : Manifest](customLayout: () => Any): String =
    renderView(customLayout, getActionClass[T], Map.empty)

  def renderView[T <: Action : Manifest](options: Map[String, Any]): String =
    renderView(layout _, getActionClass[T], options)

  def renderView[T <: Action : Manifest](): String =
    renderView(layout _, getActionClass[T], Map.empty)

  //----------------------------------------------------------------------------

  def renderViewNoLayout(location: Class[_ <: Action], options: Map[String, Any]): String =
    Config.xitrum.templateEngine match {
      case Some(engine) =>
        val ret = engine.renderView(location, this, options)
        renderedView = ret
        ret

      case None =>
        log.warn("No template engine is configured")
        ""
    }

  def renderViewNoLayout[T <: Action : Manifest](options: Map[String, Any]): String =
    renderViewNoLayout(getActionClass[T], options)

  def renderViewNoLayout[T <: Action : Manifest](): String =
    renderViewNoLayout(getActionClass[T], Map.empty)

  //----------------------------------------------------------------------------

  def renderFragment(location: Class[_ <: Action], fragment: String, options: Map[String, Any]): String =
    Config.xitrum.templateEngine match {
      case Some(engine) =>
        engine.renderFragment(location, fragment, this, options)

      case None =>
        log.warn("No template engine is configured")
        ""
    }

  def renderFragment[T <: Action : Manifest](fragment: String, options: Map[String, Any]): String =
    renderFragment(getActionClass[T], fragment, options)

  def renderFragment[T <: Action : Manifest](fragment: String): String =
    renderFragment(getActionClass[T], fragment, Map.empty)

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString()
  }
}
