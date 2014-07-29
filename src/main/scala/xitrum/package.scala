import org.slf4s.LoggerFactory
import nl.grons.metrics.scala.InstrumentedBuilder
import xitrum.metrics.MetricsManager

/**
 * Things that are usually used by application developers are put in this package
 * for convenience, because when they want to use XXX, they can simply write:
 *
 * {{{import xitrum.XXX}}}
 *
 * To avoid polluting this namespace, things that are utilities should be put in
 * package xitrum.util, not here.
 *
 * Annotations and validators are put to package xitrum.annation and xitrum.validator
 * because there are many of them. It's better for application developers to write:
 *
 * {{{
 * import xitrum.annotation._
 * import xitrum.validator._
 * }}}
 */
package object xitrum {
  lazy val version = new Version

  /**
   * Path to the root directory of the current project.
   * If you're familiar with Rails, this is the same as Rails.root.
   */
  lazy val root = {
    // Support the case when Xitrum is used in a SBT subproject, see:
    // https://github.com/xitrum-framework/xitrum/issues/47

    // Don't use application.conf, because Akka .jar file also include it
    val url = Thread.currentThread.getContextClassLoader.getResource("xitrum.conf")
    if (url != null) {
      val fileName = url.getFile
      // Use "/" instead of File.separator because "/" is always used in fileName,
      // even on Windows
      fileName.replace("/config/xitrum.conf", "")
    } else {
      // Fallback to current working directory
      System.getProperty("user.dir")
    }
  }

  /**
   * This is a convenient helper to let you call like this directly:
   * xitrum.Log.debug("msg"), xitrum.Log.info("msg") etc.
   *
   * If you do care about the class name where the log is made, use trait xitrum.Log.
   */
  // This needs to be lazy, see Server#addConfigDirectoryToClasspath
  lazy val Log = LoggerFactory.getLogger(getClass)

  lazy val Metrics = (new InstrumentedBuilder {
    val metricRegistry = MetricsManager.metricRegistry
  }).metrics
}
