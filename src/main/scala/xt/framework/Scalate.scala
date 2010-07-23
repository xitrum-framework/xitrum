package xt.framework

import java.io.{StringWriter, PrintWriter}
import org.fusesource.scalate.{TemplateEngine, Binding, DefaultRenderContext}

object Scalate {
  val engine = new TemplateEngine

  if (true) {  // production
    engine.allowReload  = false
    engine.allowCaching = true
  } else {
    engine.allowReload  = true
    engine.allowCaching = false
  }

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
