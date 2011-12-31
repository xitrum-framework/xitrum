package xitrum.view

import java.io.{ByteArrayOutputStream, PrintWriter}
import org.fusesource.scalate.{Binding, DefaultRenderContext, TemplateEngine}
import org.fusesource.scalate.scaml.ScamlOptions

import xitrum.{Config, Action}

object Scalate {
  lazy val engine = {
    val ret = new TemplateEngine
    ret.allowReload   = !Config.isProductionMode
    ScamlOptions.ugly = Config.isProductionMode
    ret.bindings      = List(Binding("helper", classOf[Action].getName, true))
    ret
  }

  def renderFile(action: Action, path: String): String = {
    val os      = new ByteArrayOutputStream
    val writer  = new PrintWriter(os)
    val context = new DefaultRenderContext(action.request.getUri, engine, writer)
    context.attributes.update("helper", action)
    engine.layout(path, context)
    writer.close()
    os.toString(Config.config.request.charset)
  }
}
