package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine => STE}
import org.fusesource.scalate.scaml.ScamlOptions
import org.fusesource.scalate.support.StringTemplateSource

import org.jboss.netty.handler.codec.serialization.ClassResolvers

import xitrum.{Config, Controller, Logger}
import xitrum.controller.Action

object Scalate extends Logger {
  private[this] val CONTROLLER_BINDING_ID = "helper"
  private[this] val CONTEXT_BINDING_ID    = "context"

  private[this] val defaultType = Config.xitrum.config.getString("scalateDefaultType")
  private[this] val dir         = Config.xitrum.config.getString("scalateDir")

  private[this] val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  private[this] val fileEngine = {
    val ret = new STE
    ret.allowCaching = true
    ret.allowReload  = !Config.productionMode
    ret
  }

  private[this] val stringEngine = {
    val ret = new STE
    ret.allowCaching = false
    ret.allowReload  = false
    ret
  }

  {
    ScamlOptions.ugly = Config.productionMode

    List(fileEngine, stringEngine).foreach { engine =>
      engine.bindings = List(
        // import Scalate utilities like "unescape"
        Binding(CONTEXT_BINDING_ID,    classOf[RenderContext].getName, true),
        // import things in the current controller
        Binding(CONTROLLER_BINDING_ID, classOf[Controller].getName,    true)
      )
    }
  }

  private def createContext(isFile: Boolean, controller: Controller, templateUri: String):
    (RenderContext, StringWriter, PrintWriter) = {
    val buffer     = new StringWriter
    val out        = new PrintWriter(buffer)
    val engine     = if (isFile) fileEngine else stringEngine
    val context    = new DefaultRenderContext(templateUri, engine, out)
    val attributes = context.attributes

    // For bindings in engine
    attributes.update(CONTEXT_BINDING_ID,    context)
    attributes.update(CONTROLLER_BINDING_ID, controller)

    // Put controller.at to context
    controller.at.foreach { case (k, v) =>
      if (k == CONTEXT_BINDING_ID || k == CONTROLLER_BINDING_ID)
        logger.warn(
          CONTEXT_BINDING_ID + " and " + CONTROLLER_BINDING_ID +
          " are reserved key names for controller's \"at\""
        )
      else
        attributes.update(k, v)
    }

    (context, buffer, out)
  }

  //----------------------------------------------------------------------------

  /**
   * Production mode: Renders the precompiled template class.
   * Development mode: Renders Scalate template file relative to dir. If the file
   * does not exist, falls back to rendering the precompiled template class.
   * @param controller will be imported in the template as "helper"
   */
  private def renderMaybePrecompiledFile(controller: Controller, relPath: String): String = {
    if (Config.productionMode)
      renderPrecompiledFile(controller, relPath)
    else
      renderNotPrecompiledFile(controller, relPath)
  }

  private def renderPrecompiledFile(controller: Controller, relPath: String): String = {
    val (context, buffer, out) = createContext(true, controller, relPath)

    // In production mode, after being precompiled
    // quickstart/controller/AppController.jade  -> class scalate.quickstart.controller.$_scalate_$AppController_jade
    // quickstart/controller/Articles/index.jade -> class scalate.quickstart.controller.Articles.$_scalate_$index_jade
    val withDots     = relPath.replace('/', '.').replace(File.separatorChar, '.')
    val xs           = withDots.split('.')
    val extension    = xs.last
    val baseFileName = xs(xs.length - 2)
    val prefix       = xs.take(xs.length - 2).mkString(".")
    val className    = "scalate." + prefix + ".$_scalate_$" + baseFileName + "_" + extension
    val klass        = classResolver.resolve(className)
    val template     = klass.asInstanceOf[Class[Template]].newInstance()
    fileEngine.layout(template, context)

    out.close()
    buffer.toString
  }

  private def renderNotPrecompiledFile(controller: Controller, relPath: String): String = {
    val path = dir + File.separator + relPath
    val file = new File(path)
    if (file.exists()) {
      val (context, buffer, out) = createContext(true, controller, relPath)
      fileEngine.layout(path, context)
      out.close()
      buffer.toString
    } else {
      // If called from a JAR library, the template may have been precompiled
      renderPrecompiledFile(controller, relPath)
    }
  }

  //----------------------------------------------------------------------------
  // For ScalateTemplateEngine

  /**
   * Renders Scalate template file at the path:
   * <scalateDir>/</class/name/of/the/controller/of/the/given/action>/<action name>.<templateType>
   *
   * @param controller will be imported in the template as "helper"
   */
  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ): String = {
    val tpe     = templateType(options)
    val relPath = controllerName.replace('.', File.separatorChar) + File.separator + actionName + "." + tpe
    Scalate.renderMaybePrecompiledFile(controller, relPath)
  }

  /**
   * Renders Scalate template file at the path:
   * <scalateDir>/</class/name/of/the/controller/of/the/given/action>.<templateType>
   *
   * @param controller will be imported in the template as "helper"
   */
  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ): String = {
    val tpe     = templateType(options)
    val relPath = controllerClass.getName.replace('.', File.separatorChar) + "." + tpe
    Scalate.renderMaybePrecompiledFile(controller, relPath)
  }

  /**
   * Renders Scalate template file with the path:
   * <scalateDir>/<the/given/controller/Class>/_<fragment>.<templateType>
   */
  def renderFragment(controller: Controller, controllerClass: Class[_], fragment: String, options: Map[String, Any] = Map()): String = {
    val tpe     = templateType(options)
    val relPath = controllerClass.getName.replace('.', File.separatorChar) + File.separatorChar + "_" + fragment + "." + tpe
    renderMaybePrecompiledFile(controller, relPath)
  }

  //----------------------------------------------------------------------------

  /** @param templateType jade, mustache, scaml, or ssp */
  def renderString(templateContent: String, templateType: String)(implicit controller: Controller): String = {
    val (context, buffer, out) = createContext(false, controller, "scalate." + templateType)
    val template               = new StringTemplateSource("scalate.jade", templateContent)
    stringEngine.layout(template, context)
    out.close()
    buffer.toString
  }

  def renderJadeString(templateContent: String)(implicit controller: Controller) =
    renderString(templateContent, "jade")(controller)

  def renderMustacheString(templateContent: String)(implicit controller: Controller) =
    renderString(templateContent, "mustache")(controller)

  def renderScamlString(templateContent: String)(implicit controller: Controller) =
    renderString(templateContent, "scaml")(controller)

  def renderSspString(templateContent: String)(implicit controller: Controller) =
    renderString(templateContent, "ssp")(controller)

  //----------------------------------------------------------------------------

  /**
   * Takes out "type" from options. It shoud be one of:
   * "jade", "mustache", "scaml", or "ssp"
   */
  private def templateType(options: Map[String, Any]) =
    options.getOrElse("type", defaultType)
}
