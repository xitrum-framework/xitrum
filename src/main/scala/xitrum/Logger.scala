package xitrum

import org.slf4j.LoggerFactory

/**
 * Although the class name can be determined by sniffing around on the stack:
 * (Thread.currentThread.getStackTrace)(2).getClassName
 *
 * We use a trait for better speed, because getStackTrace is slow.
 */
trait Logger {
  /** Logger name is inferred from the class name. */
  lazy val logger = LoggerFactory.getLogger(getClass)
}
