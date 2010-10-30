package xt.framework

import java.io.{File, StringWriter, PrintWriter}

import org.fusesource.scalate.{TemplateEngine, Binding, DefaultRenderContext}
import org.fusesource.scalate.scaml.ScamlOptions

import xt.Config
import xt.middleware.Dispatcher

object Scalate {
  val engine = new TemplateEngine
  engine.workingDirectory = new File("tmp/scalate")
  engine.bindings = List(Binding("helper", "xt.framework.Helper", true))

  if (Config.isProductionMode) {
    engine.allowReload  = false

    // Reduce the generated document size
    ScamlOptions.indent = ""
    ScamlOptions.nl     = ""

    // Significantly reduce the CPU overhead
    ScamlOptions.ugly = true
  } else {
    engine.allowReload  = true
  }

  def render(csasOrAs: String, helper: Helper): String = {
    val path     = csasOrAsToPath(csasOrAs, helper)
    val template = engine.load(path)

    val buffer = new StringWriter
    val context = new DefaultRenderContext(path, engine, new PrintWriter(buffer))
    context.attributes("helper") = helper

    template.render(context)
    buffer.toString
  }

  //----------------------------------------------------------------------------

  private def csasOrAsToPath(csasOrAs: String, helper: Helper): String = {
    val csas1 = csasOrAsToCsas(csasOrAs, helper)

    val caa = csas1.split("#")
    val controller = caa(0)
    val action     = caa(1)
    val csas2 = controller.toLowerCase.replace(".", "/") + "/" + action

    // FIXME: search viewPaths
    val path = Dispatcher.viewPaths.head.replace(".", "/")
    val extenstion = if (action.indexOf(".") != -1) "" else ".jade"
    path + "/" + csas2 + extenstion
  }

  private def csasOrAsToCsas(csasOrAs: String, helper: Helper): String = {
    if (csasOrAs.indexOf("#") == -1)
      helper.paramo("controller").getOrElse(helper.env("controller404").asInstanceOf[String]) + "#" + csasOrAs
    else
      csasOrAs
  }
}
