import org.slf4s.LoggerFactory
import nl.grons.metrics.scala.InstrumentedBuilder
import xitrum.metrics.MetricsManager

/**
 * Things that are usually used by application developers are put in this package
 * for convenience, because when they want to use XXX, they can simply write:
 *
 * {{import xitrum.XXX}}
 *
 * To avoid polluting this namespace, things that are utilities should be put in
 * package xitrum.util, not here.
 *
 * Annotations and validators are put to package xitrum.annation and xitrum.validator
 * because there are many of them. It's better for application developers to write:
 *
 * {{import xitrum.annotation._
 * import xitrum.validator._}}
 */
package object xitrum {
  val version = new Version

  /**
   * This is a convenient helper to let you call like this directly:
   * xitrum.Log.debug("msg"), xitrum.Log.info("msg") etc.
   *
   * If you do care about the class name where the log is made, use trait xitrum.Log.
   */
  val Log = LoggerFactory.getLogger(getClass)

  val Metrics = (new InstrumentedBuilder {
    val metricRegistry = MetricsManager.metricRegistry
  }).metrics
}
