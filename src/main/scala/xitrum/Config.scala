package xitrum

import java.io.File
import java.nio.charset.Charset
import java.util.{Map => JMap}

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import com.typesafe.config.{Config => TConfig, ConfigFactory}
import akka.actor.ActorSystem

import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory

import xitrum.scope.session.SessionStore
import xitrum.routing.{DiscoveredAcc, RouteCollection, RouteCollector, SerializableRouteCollection}
import xitrum.view.TemplateEngine
import xitrum.util.Loader

//------------------------------------------------------------------------------

/**
 * Dual config means the config can be in either one of the 2 forms:
 *
 * config {
 *   key = a.b.c
 * }
 *
 * Or:
 *
 * config {
 *   key {
 *     "a.b.c" {
 *       option1 = value1
 *       option2 = value2
 *     }
 *   }
 * }
 *
 */
object DualConfig {
  /** @return "a.b.c" in the description above */
  def getString(config: TConfig, key: String) = {
    val k = config.root.get(key).unwrapped  // String or java.util.Map
    if (k.isInstanceOf[String]) {
      k.toString
    } else {
      val m = k.asInstanceOf[JMap[String, Any]]
      val i = m.keySet.iterator
      i.next()
    }
  }

  /** Used when "a.b.c" is a class name. */
  def getClassInstance[T](config: TConfig, key: String): T = {
    val className = getString(config, key)
    val klass     = if (Config.productionMode) getClass.getClassLoader.loadClass(className) else DevClassLoader.load(className)
    klass.newInstance().asInstanceOf[T]
  }
}

//------------------------------------------------------------------------------

class BasicAuthConfig(config: TConfig) {
  val realm    = config.getString("realm")
  val username = config.getString("username")
  val password = config.getString("password")
}

class PortConfig(config: TConfig) {
  val http              = if (config.hasPath("http"))              Some(config.getInt("http"))              else None
  val https             = if (config.hasPath("https"))             Some(config.getInt("https"))             else None
  val flashSocketPolicy = if (config.hasPath("flashSocketPolicy")) Some(config.getInt("flashSocketPolicy")) else None
}

class HttpsConfig(config: TConfig) {
  val useOpenSSL = config.getBoolean("useOpenSSL")
  lazy val certChainFile = {
    val path = config.getString("certChainFile")
    val file = new File(path)
    if (!file.exists) Config.exitOnStartupError("certChainFile specified in xitrum.conf does not exist")
    file
  }
  lazy val keyFile = {
    val path = config.getString("keyFile")
    val file = new File(path)
    if (!file.exists) Config.exitOnStartupError("keyFile specified in xitrum.conf does not exist")
    file
  }
}

class ReverseProxyConfig(config: TConfig) {
  val ips     = config.getStringList("ips").asScala
  val baseUrl = config.getString("baseUrl")
}

class SessionConfig(config: TConfig) {
  val cookieName   = config.getString("cookieName")
  // DefaultCookie has max age of Long.MIN_VALUE by default
  val cookieMaxAge = if (config.hasPath("cookieMaxAge")) config.getLong("cookieMaxAge") else Long.MinValue
  val secureKey    = config.getString("secureKey")

  lazy val store = {
    val ret = DualConfig.getClassInstance[SessionStore](config, "store")
    ret.start()
    ret
  }
}

class StaticFileConfig(config: TConfig) {
  val pathRegex = config.getString("pathRegex").r

  val maxSizeInBytesOfCachedFiles = config.getInt("maxSizeInKBOfCachedFiles") * 1024
  val maxNumberOfCachedFiles      = config.getInt("maxNumberOfCachedFiles")

  val revalidate = config.getBoolean("revalidate")
}

class RequestConfig(config: TConfig) {
  val charsetName               = config.getString("charset")
  val charset                   = Charset.forName(charsetName)
  val maxInitialLineLength      = config.getInt("maxInitialLineLength")
  val maxSizeInBytes            = config.getLong("maxSizeInMB") * 1024 * 1024
  val maxSizeInBytesOfUploadMem =
    if (config.hasPath("maxSizeInKBOfUploadMem"))
      config.getLong("maxSizeInKBOfUploadMem") * 1024
    else
      DefaultHttpDataFactory.MINSIZE
  val tmpUploadDir              = getTmpUploadDir()
  val filteredParams            = config.getStringList("filteredParams").asScala

  private def getTmpUploadDir(): String = {
    // null means system tmp directory
    val ret = if (config.hasPath("tmpUploadDir")) config.getString("tmpUploadDir") else null

    // Check
    if (ret != null) {
      val dir = new File(ret)
      if (!dir.exists && !dir.mkdirs()) {
        throw new Exception("tmpUploadDir specified in xitrum.conf does not exist and Xitrum couldn't create it")
      } else if (!dir.isDirectory) {
        throw new Exception("tmpUploadDir specified in xitrum.conf is not a directory")
      } else if (!dir.canWrite) {
        throw new Exception("Xitrum cannot write to tmpUploadDir specified in xitrum.conf")
      }
    }
    ret
  }
}

class ResponseConfig(config: TConfig) {
  val autoGzip           = config.getBoolean("autoGzip")
  val sockJsCookieNeeded = if (config.hasPath("sockJsCookieNeeded")) config.getBoolean("sockJsCookieNeeded") else false
  val corsAllowOrigins   = if (config.hasPath("corsAllowOrigins")) config.getStringList("corsAllowOrigins").asScala else Seq.empty[String]
}

class MetricsConfig(config: TConfig) {
  import scala.concurrent.duration._

  val apiKey = if (config.hasPath("apiKey")) config.getString("apiKey") else ""

  // Xitrum default Metrics
  val jmx     = if (config.hasPath("jmx"))     config.getBoolean("jmx")     else false
  val actions = if (config.hasPath("actions")) config.getBoolean("actions") else false

  // For JMX metrics collecting
  // http://doc.akka.io/docs/akka/2.3.3/scala/cluster-usage.html
  val jmxGossipInterval        = (if (config.hasPath("jmxGossipInterval"))        config.getInt("jmxGossipInterval")        else 3).seconds
  val jmxMovingAverageHalfLife = (if (config.hasPath("jmxMovingAverageHalfLife")) config.getInt("jmxMovingAverageHalfLife") else 12).seconds

  // For metrics collect actor schedule
  val collectActorInitialDelay = (if (config.hasPath("collectActorInitialDelay")) config.getLong("collectActorInitialDelay") else 1).seconds
  val collectActorInterval     = (if (config.hasPath("collectActorInterval"))     config.getLong("collectActorInterval")     else 30).seconds
}

/** This represents things in xitrum.conf. */
class XitrumConfig(val config: TConfig) {
  val basicAuth =
    if (config.hasPath("basicAuth"))
      Some(new BasicAuthConfig(config.getConfig("basicAuth")))
    else
      None

  val interface =
    if (config.hasPath("interface"))
      Some(config.getString("interface"))
    else
      None

  val port = new PortConfig(config.getConfig("port"))

  val https = if (port.https.isDefined) Some(new HttpsConfig(config.getConfig("https"))) else None

  val reverseProxy =
    if (config.hasPath("reverseProxy"))
      Some(new ReverseProxyConfig(config.getConfig("reverseProxy")))
    else
      None

  val staticFile = new StaticFileConfig(config.getConfig("staticFile"))

  val request = new RequestConfig(config.getConfig("request"))

  val response = new ResponseConfig(config.getConfig("response"))

  val swaggerApiVersion = if (config.hasPath("swaggerApiVersion")) Some(config.getString("swaggerApiVersion")) else None

  val metrics = if (config.hasPath("metrics")) Some(new MetricsConfig(config.getConfig("metrics"))) else None

  //----------------------------------------------------------------------------
  // These are initialized after xitrum.Config, so that they can use xitrum.Config.

  var template: Option[TemplateEngine] = _
  var cache:    Cache                  = _
  var session:  SessionConfig          = _

  def loadExternalEngines() {
    template = TemplateEngine.loadFromConfig()
    cache    = Cache.loadFromConfig()
    session  = new SessionConfig(config.getConfig("session"))
  }
}

//------------------------------------------------------------------------------

/** See config/xitrum.properties */
object Config {
  val ACTOR_SYSTEM_NAME = "xitrum"

  /** akka.actor.ActorSystem("xitrum") */
  val actorSystem = ActorSystem(ACTOR_SYSTEM_NAME)

  /**
   * Static textual files are always compressed
   * Dynamic textual responses are only compressed if they are big
   * http://code.google.com/speed/page-speed/docs/payload.html#GzipCompression
   *
   * Google recommends > 150B-1KB
   */
  val BIG_TEXTUAL_RESPONSE_SIZE_IN_KB = 1

  /**
   * The default secure key in Xitrum new project skeleton xitrum.conf. At a
   * program startup, Xitrum will warn if the program uses this key.
   */
  private[this] val DEFAULT_SECURE_KEY = "ajconghoaofuxahoi92chunghiaujivietnamlasdoclapjfltudoil98hanhphucup8"

  //----------------------------------------------------------------------------

  /**
   * true if "xitrum.mode" system property is set to "production"
   * See bin/runner.sh.
   */
  val productionMode = System.getProperty("xitrum.mode") == "production"

  /**
   * true by default. Set to false (typically at your program's boot) if you
   * want to disable class autoreload feature in development mode. This is only
   * neccessary in very special cases if you have problem with autoreload.
   */
  var autoreloadInDevMode = true

  /** This represents application.conf. */
  val application: TConfig = {
    try {
      // See Server#addConfigDirectoryToClasspath
      //
      // Unlike ConfigFactory.load(), when class loader is given but
      // "application" is not given, "application.conf" is not loaded!
      ConfigFactory.load(getClass.getClassLoader, "application")
    } catch {
      case NonFatal(e) =>
        exitOnStartupError("Could not load config/application.conf. For an example, see https://github.com/xitrum-framework/xitrum-new/blob/master/config/application.conf", e)
        throw e
    }
  }

  /** This represents things in xitrum.conf. */
  val xitrum: XitrumConfig = {
    try {
      new XitrumConfig(application.getConfig("xitrum"))
    } catch {
      case NonFatal(e) =>
        exitOnStartupError("Could not load config/xitrum.conf. For an example, see https://github.com/xitrum-framework/xitrum-new/blob/master/config/xitrum.conf", e)
        throw e
    }
  }

  //----------------------------------------------------------------------------

  val baseUrl = xitrum.reverseProxy.map(_.baseUrl).getOrElse("")

  def withBaseUrl(path: String): String = {
    // Avoid returning path with double "//" prefix.
    // Something like:
    // //xitrum/postback/zOIc0v...
    // will cause the browser to send request to http://xitrum/postback/zOIc0v...
    if (baseUrl.isEmpty) {
      if (path.isEmpty)
        "/"
      else if (path.startsWith("/"))
        path
      else
        "/" + path
    } else {
      if (path.isEmpty)
        baseUrl
      else if (path.startsWith("/"))
        baseUrl + path
      else
        baseUrl + "/" + path
    }
  }

  //----------------------------------------------------------------------------

  def warnOnDefaultSecureKey() {
    if (xitrum.session.secureKey == DEFAULT_SECURE_KEY)
      Log.warn("*** For security, change secureKey in config/xitrum.conf to your own! ***")
  }

  def exitOnStartupError(msg: String) {
    exitOnStartupError(msg, None)
  }

  def exitOnStartupError(msg: String, e: Throwable) {
    exitOnStartupError(msg, Some(e))
  }

  private def exitOnStartupError(msg: String, eo: Option[Throwable]) {
    eo.foreach { e => Log.error(msg, e) }
    Log.error("Xitrum could not start because of the above error. Xitrum will now stop the current process.")

    // Note: If the cache is Hazelcast, once it's started, calling only
    // System.exit(-1) does not stop the current process!
    System.exit(-1)
  }

  //----------------------------------------------------------------------------

  private[this] val ROUTES_CACHE = "routes.cache"

  var routes = loadRoutes(getClass.getClassLoader)

  /** Maybe called multiple times in development mode when reloading routes. */
  def loadRoutes(cl: ClassLoader): RouteCollection = {
    val ret = loadRouteCacheFileOrRecollectWithRetry(cl)
    if (xitrum.metrics.isEmpty) ret.removeByPrefix("xitrum/metrics")
    ret
  }

  private def loadRouteCacheFileOrRecollectWithRetry(cl: ClassLoader, retried: Boolean = false): RouteCollection = {
    try {
      Log.info(s"Load $ROUTES_CACHE or recollect routes...")
      val routeCollector = new RouteCollector(cl, ROUTES_CACHE)
      val discoveredAcc  = routeCollector.deserializeCacheFileOrRecollect()
      if (discoveredAcc.xitrumVersion != _root_.xitrum.version.toString) {
        Log.info(s"Xitrum version changed. Delete $ROUTES_CACHE and retry...")
        val file = new File(ROUTES_CACHE)
        file.delete()
        loadRouteCacheFileOrRecollectWithRetry(cl, true)
      } else {
        val withSwagger = xitrum.swaggerApiVersion.isDefined
        RouteCollection.fromSerializable(cl, discoveredAcc, withSwagger)
      }
    } catch {
      case NonFatal(e) =>
        if (retried) {
          Config.exitOnStartupError("Could not collect routes", e)
          throw e
        } else {
          Log.info(s"Could not load $ROUTES_CACHE, delete and retry...")
          val file = new File(ROUTES_CACHE)
          file.delete()
          loadRouteCacheFileOrRecollectWithRetry(cl, true)
        }
    }
  }
}
