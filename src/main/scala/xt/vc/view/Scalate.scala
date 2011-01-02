package xt.vc.view

import xt.{Config, Controller}

import java.io.{File, StringWriter, PrintWriter}

import org.fusesource.scalate.{TemplateEngine, Binding, DefaultRenderContext}
import org.fusesource.scalate.scaml.ScamlOptions

object Scalate {
  val SCALATE_EXTENSIONS    = Array("jade", "scaml", "ssp", "mustache", "html")
  val DEFAULT_EXTENSION     = "jade"

  // templates are compiles to packaged .classes
  // => use special prefix to avoid package name conflict
  val VIEW_DIRECTORY_PREFIX = "view"

  private lazy val engine = {
    val ret = new TemplateEngine

    ret.workingDirectory = new File("tmp/scalate")
    ret.bindings = List(Binding("controller", "xt.vc.Controller", true))

    // See http://scalate.fusesource.org/documentation/scalate-embedding-guide.html#custom_template_loading
    if (Config.isProductionMode) {
      ret.allowReload = false

      // Reduce the generated document size
      ScamlOptions.indent = ""
      ScamlOptions.nl     = ""

      // Significantly reduce the CPU overhead
      ScamlOptions.ugly = true
    } else {
      ret.allowReload = true
    }

    ret
  }

  def render(controller: Controller, csasOrAs: String): String = {
    val path     = findTemplate(controller, csasOrAs)
    val template = engine.load(path)

    val buffer = new StringWriter
    val context = new DefaultRenderContext(path, engine, new PrintWriter(buffer))
    context.attributes("controller") = controller

    template.render(context)
    buffer.toString
  }

  //----------------------------------------------------------------------------

  private def findTemplate(controller: Controller, csasOrAs: String): String = {
    val viewPath = if (csasOrAs.indexOf("#") == -1) {
      val actionName = csasOrAs
      val className  = controller.getClass.getName
      className.replace(".", File.separator) + File.separator + actionName
    } else {
      csasOrAs.replace(".", File.separator).replace("#", File.separator)
    }

    // Append extension if necessary
    val viewPath2 = if (SCALATE_EXTENSIONS.exists(ext => viewPath.endsWith("." + ext))) {
      viewPath
    } else {
      viewPath + "." + DEFAULT_EXTENSION
    }

    val viewPath3 = VIEW_DIRECTORY_PREFIX + File.separator + viewPath2

    // When running in development mode ("sbt run"), relPath is relative to
    // target/scala_<VERSION>/resources
    //
    // To make Scalate reload the template when there is modification, we need
    // to make relPath relative to
    // src/main/resources
    if (Config.isProductionMode) {
      viewPath3
    } else {
      System.getProperty("user.dir") + File.separator +
      "src" + File.separator + "main" + File.separator + "resources" + File.separator +
      viewPath3
    }
  }
}
