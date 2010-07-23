package xt.framework

import java.io.{StringWriter, PrintWriter}
import org.fusesource.scalate.{TemplateEngine, Binding, DefaultRenderContext}

object Scalate {
  val engine = new TemplateEngine
  engine.allowReload  = false
  engine.allowCaching = true
  engine.bindings = List(Binding("helper", "xt.framework.View", true))

  def render(view: View, path: String): String = {
    val template = engine.load(path)

    val buffer  = new StringWriter
    val context = new DefaultRenderContext(engine, new PrintWriter(buffer))

    context.attributes("helper") = view
    template.render(context)
    buffer.toString
  }
}
