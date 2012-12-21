package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine}
import org.fusesource.scalate.scaml.ScamlOptions
import org.fusesource.scalate.support.StringTemplateSource
import org.jboss.netty.handler.codec.serialization.ClassResolvers

import xitrum.{Config, Controller, Logger}

object Scalate extends Logger {
  // build.sbt of Xitrum app should have this line:
  // scalateTemplateDirectory in Compile <<= (baseDirectory) { _ / "src/main/view" }
  private[this] val TEMPLATE_DIR          = "src/main/view/scalate"

  // build.sbt of Xitrum app should have this line:
  // scalateBindings += Binding("helper", "xitrum.Controller", true)
  private[this] val CONTROLLER_BINDING_ID = "helper"

  private[this] val CONTEXT_BINDING_ID    = "context"

  private[this] val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  private[this] val fileEngine = {
    val ret = new TemplateEngine
    ret.allowCaching = true
    ret.allowReload  = !Config.isProductionMode
    ret
  }

  private[this] val stringEngine = {
    val ret = new TemplateEngine
    ret.allowCaching = false
    ret.allowReload  = false
    ret
  }

  {
    ScamlOptions.ugly = Config.isProductionMode

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
   * Renders Scalate template file relative to src/main/view/scalate directory.
   * @param controller will be imported in the template as "helper"
   */
  def renderFile(controller: Controller, relPath: String): String = {
    val (context, buffer, out) = createContext(true, controller, relPath)

    if (Config.isProductionMode) {
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
    } else {
      val path = TEMPLATE_DIR + File.separator + relPath
      fileEngine.layout(path, context)
    }

    out.close()
    buffer.toString
  }

  /** @param templateType jade, mustache, scaml, ssp */
  def renderString(controller: Controller, templateContent: String, templateType: String): String = {
    val (context, buffer, out) = createContext(false, controller, "scalate." + templateType)
    val template               = new StringTemplateSource("scalate.jade", templateContent)
    stringEngine.layout(template, context)
    out.close()
    buffer.toString
  }

  def renderJadeString(controller: Controller, templateContent: String) =
    renderString(controller, templateContent, "jade")

  def renderMustacheString(controller: Controller, templateContent: String) =
    renderString(controller, templateContent, "mustache")

  def renderScamlString(controller: Controller, templateContent: String) =
    renderString(controller, templateContent, "scaml")

  def renderSspString(controller: Controller, templateContent: String) =
    renderString(controller, templateContent, "ssp")
}
