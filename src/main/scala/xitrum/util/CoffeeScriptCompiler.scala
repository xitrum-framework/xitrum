package xitrum.util

import javax.script.ScriptException
import tv.cntt.rhinocoffeescript.Compiler
import xitrum.Logger

object CoffeeScriptCompiler extends Logger {
  def compile(coffeeScript: String): Option[String] = {
    try {
      val javaScript = Compiler.compile(coffeeScript)
      Some(javaScript)
    } catch {
      case e: ScriptException =>
        logger.warn("CoffeeScript syntax error at %d:%d".format(e.getLineNumber, e.getColumnNumber))
        None
    }
  }
}
