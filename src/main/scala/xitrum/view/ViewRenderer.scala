package xitrum.view

import xitrum.{Action, Config}

trait ViewRenderer extends GetActionClassDefaultsToCurrentAction {
  this: Action =>

  var renderedView: Any = _

  def layout: Any = renderedView

  //----------------------------------------------------------------------------

  /**
   * Renders the template at ``uri`` ([[renderedView]] is not affected).
   *
   * @param options specific to the configured template engine
   */
  def renderTemplate(uri: String, options: Map[String, Any]): String =
    Config.xitrum.template match {
      case Some(engine) =>
        engine.renderTemplate(uri, this, options)

      case None =>
        throw new IllegalStateException("No template engine is configured")
    }

  /**
   * Renders the template at ``uri`` ([[renderedView]] is not affected).
   */
  def renderTemplate(uri: String): String =
    renderTemplate(uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template at ``uri`` to [[renderedView]].
   *
   * @param options specific to the configured template engine
   */
  def renderViewNoLayout(uri: String, options: Map[String, Any]): String = {
    val ret = renderTemplate(uri, options)
    renderedView = ret
    ret
  }

  /**
   * Renders the template at ``uri`` to [[renderedView]].
   */
  def renderViewNoLayout(uri: String): String =
    renderViewNoLayout(uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]].
   *
   * @param options specific to the configured template engine
   */
  def renderViewNoLayout(actionClass: Class[_ <: Action], options: Map[String, Any]): String =
    renderViewNoLayout(templateUriFromClass(actionClass), options)

  /**
   * Renders the template associated with the action to [[renderedView]].
   */
  def renderViewNoLayout(actionClass: Class[_ <: Action]): String =
    renderViewNoLayout(templateUriFromClass(actionClass), Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]].
   *
   * @param options specific to the configured template engine
   */
  def renderViewNoLayout[T <: Action: Manifest](options: Map[String, Any]): String =
    renderViewNoLayout(getActionClass[T], options)

  /**
   * Renders the template associated with the action to [[renderedView]].
   */
  def renderViewNoLayout[T <: Action: Manifest](): String =
    renderViewNoLayout(getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template at ``uri`` to [[renderedView]], then calls the custom layout.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, uri: String, options: Map[String, Any]): String = {
    renderViewNoLayout(uri, options)
    customLayout.apply().toString
  }

  /**
   * Renders the template at ``uri`` to [[renderedView]], then calls the custom layout.
   */
  def renderView(customLayout: () => Any, uri: String): String = {
    renderView(customLayout, uri, Map.empty[String, Any])
  }

  //----------------------------------------------------------------------------

  /**
   * Renders the template at ``uri`` to [[renderedView]], then calls [[layout]].
   *
   * @param options specific to the configured template engine
   */
  def renderView(uri: String, options: Map[String, Any]): String =
    renderView(layout _, uri, options)

  /**
   * Renders the template at ``uri`` to [[renderedView]], then calls [[layout]].
   */
  def renderView(uri: String): String =
    renderView(layout _, uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls the custom layout.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, actionClass: Class[_ <: Action], options: Map[String, Any]): String = {
    renderView(customLayout, templateUriFromClass(actionClass), options)
  }

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls the custom layout.
   */
  def renderView(customLayout: () => Any, actionClass: Class[_ <: Action]): String = {
    renderView(customLayout, templateUriFromClass(actionClass), Map.empty[String, Any])
  }

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls the custom layout.
   *
   * @param options specific to the configured template engine
   */
  def renderView[T <: Action: Manifest](customLayout: () => Any, options: Map[String, Any]): String =
    renderView(customLayout, getActionClass[T], options)

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls the custom layout.
   */
  def renderView[T <: Action: Manifest](customLayout: () => Any): String =
    renderView(customLayout, getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls [[layout]].
   *
   * @param options specific to the configured template engine
   */
  def renderView(actionClass: Class[_ <: Action], options: Map[String, Any]): String = {
    renderView(layout _, templateUriFromClass(actionClass), options)
  }

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls [[layout]].
   */
  def renderView(actionClass: Class[_ <: Action]): String = {
    renderView(layout _, templateUriFromClass(actionClass), Map.empty[String, Any])
  }

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls [[layout]].
   *
   * @param options specific to the configured template engine
   */
  def renderView[T <: Action: Manifest](options: Map[String, Any]): String =
    renderView(layout _, getActionClass[T], options)

  /**
   * Renders the template associated with the action to [[renderedView]],
   * then calls [[layout]].
   */
  def renderView[T <: Action: Manifest](): String =
    renderView(layout _, getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
    * Sets [[renderedView]] and call [[layout]].
    */
  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout method
    any.toString
  }

  //----------------------------------------------------------------------------

  /**
   * Renders the template fragment at the directory.
   *
   * @param options specific to the configured template engine
   */
  def renderFragment(directoryUri: String, fragment: String, options: Map[String, Any]): String =
    renderTemplate(directoryUri + "/_" + fragment, options)

  /**
   * Renders the template fragment at the directory.
   */
  def renderFragment(directoryUri: String, fragment: String): String =
    renderFragment(directoryUri, fragment, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template fragment at the directory associated with the action.
   *
   * @param options specific to the configured template engine
   */
  def renderFragment(actionClass: Class[_ <: Action], fragment: String, options: Map[String, Any]): String =
    renderFragment(fragmentDirectoryUriFromClass(actionClass), fragment, options)

  /**
   * Renders the template fragment at the directory associated with the action.
   */
  def renderFragment(actionClass: Class[_ <: Action], fragment: String): String =
    renderFragment(actionClass, fragment, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /**
   * Renders the template fragment at the directory associated with the action.
   *
   * @param options specific to the configured template engine
   */
  def renderFragment[T <: Action: Manifest](fragment: String, options: Map[String, Any]): String =
    renderFragment(getActionClass[T], fragment, options)

  /**
   * Renders the template fragment at the directory associated with the action.
   */
  def renderFragment[T <: Action: Manifest](fragment: String): String =
    renderFragment(getActionClass[T], fragment, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  /** Converts, for example, a.b.C to a/b/C. */
  private def templateUriFromClass(klass: Class[_]): String = {
    // Scalate expects URI thus doesn't work with File.separatorChar on Windows
    klass.getName.replace('.', '/')
  }

  /** Converts, for example, a.b.C to a/b. */
  private def fragmentDirectoryUriFromClass(klass: Class[_]): String = {
    // klass.getPackage only returns non-null if the current ClassLoader is already aware of the package.
    //
    // Scalate expects URI thus doesn't work with File.separator on Windows.
    klass.getName.split('.').dropRight(1).mkString("/")
  }
}
