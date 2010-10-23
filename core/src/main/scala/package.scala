import org.slf4j.LoggerFactory

package object xt {
	/**
	 * Get the logger for the caller's class name. The class name is determined by
	 * sniffing around on the stack.
	 */
  def logger = {
		val stackTraceElements = Thread.currentThread.getStackTrace
		val element = stackTraceElements(2)
		val className = element.getClassName

		// Scala's object
		val className2 = if (className.endsWith("$"))
      className.substring(0, className.length - 1)
    else
      className

		LoggerFactory.getLogger(className2)
	}

  def logger(name: String) = LoggerFactory.getLogger(name)
}
