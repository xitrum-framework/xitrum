package xitrum.view

import java.io.{File, ByteArrayOutputStream, PrintWriter}

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
    val os      = new ByteArrayOutputStream
    val writer  = new PrintWriter(os)
    val context = new DefaultRenderContext(relPath, engine, writer)
    context.attributes.update(CONTROLLER_BINDING_ID, controller)
    engine.layout(path, context)
    writer.close()
    os.toString(Config.config.request.charset)
  }
}
