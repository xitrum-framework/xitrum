package xt

import org.slf4j.LoggerFactory

/**
 * Although the class name can be determined by sniffing around on the stack:
 * (Thread.currentThread.getStackTrace)(2).getClassName
 *
 * We use a trait for better speed, because getStackTrace is slow.
 */
trait Logger {
  /**
   * By default, the logger name is inferred from the class name.
   */
  var loggerName: String = null

  val logger = {
    if (loggerName == null) loggerName = inferLoggerNameFromClassName
    LoggerFactory.getLogger(loggerName)
  }

  private def inferLoggerNameFromClassName: String = {
    val className = getClass.getName

    // Remove trailing "$"
    // Object that ends with "$" is typically Scala's object
    if (className.endsWith("$"))
      className.substring(0, className.length - 1)
    else
      className
  }
}
