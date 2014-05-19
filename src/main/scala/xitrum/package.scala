import org.slf4j.LoggerFactory
import nl.grons.metrics.scala.InstrumentedBuilder
import xitrum.metrics.MetricsManager

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
