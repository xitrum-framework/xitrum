package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, TemplateEngine}
import org.fusesource.scalate.scaml.ScamlOptions

import xitrum.{Config, Controller}

object Scalate {
  private val DIR                   = "src/main/view/scalate"
  private val CONTROLLER_BINDING_ID = "helper"

  private lazy val engine = {
    val ret = new TemplateEngine
    ret.allowReload   = !Config.isProductionMode
    ScamlOptions.ugly = Config.isProductionMode
    ret.bindings      = List(Binding(CONTROLLER_BINDING_ID, classOf[Controller].getName, true))
    ret
  }

  /**
   * Renders Scalate template file relative to src/main/view/scalate directory.
   * @param controller will be imported in the template as "helper"
   */
  def renderFile(controller: Controller, relPath: String): String = {
    val path    = DIR + File.separator + relPath
    val buffer  = new StringWriter
    val out     = new PrintWriter(buffer)
    val context = new DefaultRenderContext(relPath, engine, out)
    context.attributes.update(CONTROLLER_BINDING_ID, controller)
    engine.layout(path, context)
    out.close()
    buffer.toString
  }
}
