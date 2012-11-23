package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine}
import org.fusesource.scalate.scaml.ScamlOptions
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

  private lazy val engine = {
    val ret = new TemplateEngine
    ret.allowReload   = !Config.isProductionMode
    ScamlOptions.ugly = Config.isProductionMode
    ret.bindings      = List(
      // import Scalate utilities like "unescape"
      Binding(CONTEXT_BINDING_ID,    classOf[RenderContext].getName, true),
      // import things in the current controller
      Binding(CONTROLLER_BINDING_ID, classOf[Controller].getName,    true)
    )
    ret
  }

  /**
   * Renders Scalate template file relative to src/main/view/scalate directory.
   * @param controller will be imported in the template as "helper"
   */
  def renderFile(controller: Controller, relPath: String): String = {
    val buffer     = new StringWriter
    val out        = new PrintWriter(buffer)
    val context    = new DefaultRenderContext(relPath, engine, out)
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
      engine.layout(template, context)
    } else {
      val path = TEMPLATE_DIR + File.separator + relPath
      engine.layout(path, context)
    }

    out.close()
    buffer.toString
  }
}
