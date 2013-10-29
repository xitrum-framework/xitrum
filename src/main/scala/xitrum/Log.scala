package xitrum

import org.slf4j.LoggerFactory

/**
 * Although the class name can be determined by sniffing around on the stack:
 * (Thread.currentThread.getStackTrace)(2).getClassName
 *
 * We use a trait for better speed, because getStackTrace is slow.
 */
trait Log {
  /** Log name is inferred from the class name. */
  lazy val log = LoggerFactory.getLogger(getClass)
}
