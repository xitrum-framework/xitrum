package xitrum.metrics

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper

import akka.actor.{Actor, ActorRef, Cancellable, ExtendedActorSystem, Props, Terminated}
import akka.cluster.metrics.{ClusterMetricsChanged, JmxMetricsCollector, NodeMetrics}
import glokka.Registry

import xitrum.Config

case object Subscribe
case object UnSubscribe
case object Pull
case object Collect
case class Publish(registryAsJson: String)

/** For convenience, you can use `xitrum.Metrics`. */
object MetricsManager {
  // implicit executer for scheduled message
  import Config.actorSystem.dispatcher

  val PUBLISHER = "XitrumMetricsPublisher"

  lazy val metrics = Config.xitrum.metrics.get

  // Glokka registry
  val actorRegistry = Registry.start(Config.actorSystem, getClass.getName)

  // For non-clusterd actor system
  // http://grokbase.com/t/gg/akka-user/136m8mmyed/get-address-of-the-actorsystem-in-akka-2-2
  private val provider = Config.actorSystem.asInstanceOf[ExtendedActorSystem].provider
  private val address  = provider.getDefaultAddress

  // http://doc.akka.io/docs/akka/2.3.3/scala/cluster-usage.html
  lazy val jmx = new JmxMetricsCollector(address, EWMA.alpha(metrics.jmxMovingAverageHalfLife, metrics.jmxGossipInterval))

  // For metrics of application
  val metricRegistry = new MetricRegistry

  // For json metrics reporting
  private val module  = new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, true)
  private val mapper  = (new ObjectMapper).registerModule(module)
  def registryAsJson  = {
    // Add address of current node into JSON
    // TODO: Make this better!
    mapper.writeValueAsString(metricRegistry).patch(0, s"""{"TYPE": "metrics", "address": "$address", """, 1)
  }

  var collector:      ActorRef    = _
  var publisher:      ActorRef    = _
  var localPublisher: ActorRef    = _
  var collecting:     Cancellable = _
  var publishing:     Cancellable = _

  def start() {
    localPublisher = Config.actorSystem.actorOf(Props(classOf[LocalPublisher]))
    tickPublisher(localPublisher)

    if (metrics.jmx) {
      if (provider.getClass.getName == "akka.cluster.ClusterActorRefProvider") {
        Config.actorSystem.actorOf(Props(classOf[ClusterMetricsCollector], localPublisher))
      } else {
        collector  = Config.actorSystem.actorOf(Props(classOf[MetricsCollector], localPublisher))
        collecting = Config.actorSystem.scheduler.schedule(
          metrics.collectActorInitialDelay,
          metrics.collectActorInterval,
          collector,
          Collect
        )
      }
    }
  }

  def stop() {
    if (collector  != null) collector ! UnSubscribe
    if (collecting != null) collecting.cancel()
    if (publishing != null) publishing.cancel()
  }

  /**
   * Tick registry to publisher
   */
  def tickPublisher(publisher: ActorRef) = {
    // Send newest registry as JSON
    publishing = Config.actorSystem.scheduler.schedule(
      metrics.collectActorInterval + 1.seconds,  // Publish after first collect execution
      metrics.collectActorInterval
    )({
      publisher ! Publish(registryAsJson)
    })
  }

  // The exponentially weighted moving average (EWMA)
  // Hard copy from
  // https://github.com/akka/akka/blob/master/akka-cluster/src/main/scala/akka/cluster/ClusterMetricsCollector.scala
  private object EWMA {
    private val LogOf2 = 0.69315  // math.log(2)

    /**
     * Calculate the alpha (decay factor) used in [[akka.cluster.EWMA]]
     * from specified half-life and interval between observations.
     * Half-life is the interval over which the weights decrease by a factor of two.
     * The relevance of each data sample is halved for every passing half-life duration,
     * i.e. after 4 times the half-life, a data sample's relevance is reduced to 6% of
     * its original relevance. The initial relevance of a data sample is given by
     * 1 - 0.5 ^ (collect-interval / half-life).
     */
    def alpha(halfLife: FiniteDuration, collectInterval: FiniteDuration): Double = {
      val halfLifeMillis = halfLife.toMillis
      require(halfLife.toMillis > 0, "halfLife must be > 0 s")
      val decayRate = LogOf2 / halfLifeMillis
      1 - math.exp(-decayRate * collectInterval.toMillis)
    }
  }

  /**
   * Publisher for local node.
   * Relay messages to MetricsPublisher.
   */
  private class LocalPublisher extends Actor with PublisherLookUp {
    override def preStart() {
      lookUpPublisher()
    }

    def receive = {
      case _ =>
    }

    override def doWithPublisher(globalPublisher: ActorRef) = {
      context.watch(globalPublisher)
      context.become {
        case Publish(registryAsJson) =>
          globalPublisher ! Publish(registryAsJson)

        case nodeMetrics: NodeMetrics =>
          globalPublisher ! nodeMetrics

        case m: ClusterMetricsChanged =>
          globalPublisher ! m

        case Terminated(publisher) =>
          lookUpPublisher()

        case _ =>
      }
    }
  }
}

/** Common code for looking up MetricsPublisher actor from Glokka. */
trait PublisherLookUp {
  this: Actor =>

  def lookUpPublisher() {
    val props = Props[MetricsPublisher]
    MetricsManager.actorRegistry ! Registry.Register(MetricsManager.PUBLISHER, props)
    context.become {
      case msg: Registry.FoundOrCreated => doWithPublisher(msg.ref)
      case _ =>
    }
  }

  def doWithPublisher(publisher: ActorRef)
}
