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
    LoggerFactory.getLogger(getClass)
  }
}
