package xitrum.util

import java.util.{Collections, LinkedHashMap, Map}
import javax.script.ScriptException
import tv.cntt.rhinocoffeescript.Compiler
import xitrum.Log

/**
 * Compiled script is cached. Cache with size 1024. Least recently used element
 * is removed first.
 */
object CoffeeScriptCompiler {
  // http://www.java-blog.com/creating-simple-cache-java-linkedhashmap-anonymous-class

  private[this] val MAX_CACHE_SIZE = 1024

  private[this] val cache = Collections.synchronizedMap(
    new LinkedHashMap[String, String](MAX_CACHE_SIZE, 1.0f, true) {
      protected override def removeEldestEntry(eldest: Map.Entry[String, String]) =
        size() > MAX_CACHE_SIZE * 0.9
    }
  )

  def compile(coffeeScript: String): Option[String] = {
    val cachedJavaScript = cache.get(coffeeScript)
    if (cachedJavaScript != null) return Some(cachedJavaScript)

    try {
      val javaScript = Compiler.compile(coffeeScript)
      cache.put(coffeeScript, javaScript)
      Some(javaScript)
    } catch {
      case e: ScriptException =>
        Log.error("CoffeeScript syntax error at %d:%d".format(e.getLineNumber, e.getColumnNumber))
        None
    }
  }
}
