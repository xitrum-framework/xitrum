package xitrum

import java.io.File
import java.nio.charset.Charset
import scala.util.control.NonFatal

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.XmlClientConfigBuilder
import com.hazelcast.core.{Hazelcast, HazelcastInstance}

import com.typesafe.config.{Config => TConfig, ConfigFactory}
import akka.actor.ActorSystem

import xitrum.scope.session.SessionStore
import xitrum.routing.{DiscoveredAcc, RouteCollection, RouteCollector, SerializableRouteCollection, SockJsClassAndOptions}
import xitrum.view.TemplateEngine
import xitrum.util.Loader

//----------------------------------------------------------------------------

class BasicAuthConfig(config: TConfig) {
  val realm    = config.getString("realm")
  val username = config.getString("username")
  val password = config.getString("password")
}

class PortConfig(config: TConfig) {
  val http  = if (config.hasPath("http"))  Some(config.getInt("http"))  else None
  val https = if (config.hasPath("https")) Some(config.getInt("https")) else None
  val flashSocketPolicy =
    if (config.hasPath("flashSocketPolicy"))
      Some(config.getInt("flashSocketPolicy"))
    else
      None
}

class KeystoreConfig(config: TConfig) {
  val path                = config.getString("path")
  val password            = config.getString("password")
  val certificatePassword = config.getString("certificatePassword")
}

class ReverseProxyConfig(config: TConfig) {
  val ips     = config.getStringList("ips")
  val baseUrl = config.getString("baseUrl")
}

class SessionConfig(config: TConfig) {
  val store      = config.getString("store")
  val cookieName = config.getString("cookieName")
  val secureKey  = config.getString("secureKey")
}

class RequestConfig(config: TConfig) {
  val charsetName    = config.getString("charset")
  val maxSizeInMB    = config.getInt("maxSizeInMB")
  val filteredParams = config.getStringList("filteredParams")

  // Starts and stops with "/", like "/static/", if any
  val staticFileUrlPrefix = if (config.hasPath("staticFileUrlPrefix")) Some(config.getString("staticFileUrlPrefix")) else None

  val charset = Charset.forName(charsetName)
}

class ResponseConfig(config: TConfig) {
  val autoGzip = config.getBoolean("autoGzip")

  val maxSizeInKBOfCachedStaticFiles = config.getInt("maxSizeInKBOfCachedStaticFiles")
  val maxNumberOfCachedStaticFiles   = config.getInt("maxNumberOfCachedStaticFiles")

  val clientMustRevalidateStaticFiles = config.getBoolean("clientMustRevalidateStaticFiles")
}

class Config(val config: TConfig) extends Logger {
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

  val keystore = new KeystoreConfig(config.getConfig("keystore"))

  val reverseProxy =
    if (config.hasPath("reverseProxy"))
      Some(new ReverseProxyConfig(config.getConfig("reverseProxy")))
    else
      None

  /**
   * lazy: templateEngine is initialized after xitrum.Config, so that inside
   * the template engine class, xitrum.Config can be used
   */
  lazy val templateEngine: TemplateEngine = {
    if (config.hasPath("templateEngine")) {
      val className = config.getString("templateEngine")
      try {
        val klass = Class.forName(className)
        klass.newInstance().asInstanceOf[TemplateEngine]
      } catch {
        case NonFatal(e) =>
          Config.exitOnError("Could not load template engine, please check config/xitrum.conf", e)
          null
      }
    } else {
      logger.info("No template engine is configured")
      null
    }
  }

  val hazelcastMode = config.getString("hazelcastMode")

  val session = new SessionConfig(config.getConfig("session"))

  val swaggerApiVersion = if (config.hasPath("swaggerApiVersion")) Some(config.getString("swaggerApiVersion")) else None

  val request = new RequestConfig(config.getConfig("request"))

  val response = new ResponseConfig(config.getConfig("response"))
}

//----------------------------------------------------------------------------

/** See config/xitrum.properties */
object Config extends Logger {
  val ACTOR_SYSTEM_NAME = "xitrum"

  /**
   * Static textual files are always compressed
   * Dynamic textual responses are only compressed if they are big
   * http://code.google.com/speed/page-speed/docs/payload.html#GzipCompression
   *
   * Google recommends > 150B-1KB
   */
  val BIG_TEXTUAL_RESPONSE_SIZE_IN_KB = 1

  private[this] val HAZELCAST_MODE_CLUSTER_MEMBER = "clusterMember"
  private[this] val HAZELCAST_MODE_JAVA_CLIENT    = "javaClient"

  private[this] val DEFAULT_SECURE_KEY = "ajconghoaofuxahoi92chunghiaujivietnamlasdoclapjfltudoil98hanhphucup8"

  //----------------------------------------------------------------------------

  /**
   * true if "xitrum.mode" system property is set to "production"
   * See bin/runner.sh.
   */
  val productionMode = System.getProperty("xitrum.mode") == "production"

  /** config/application.conf */
  val application: TConfig = {
    try {
      ConfigFactory.load()
    } catch {
      case NonFatal(e) =>
        exitOnError("Could not load config/application.conf. For an example, see https://github.com/ngocdaothanh/xitrum-new/blob/master/config/application.conf", e)
        null
    }
  }

  /** config/xitrum.conf */
  val xitrum: Config = {
    try {
      new Config(application.getConfig("xitrum"))
    } catch {
      case NonFatal(e) =>
        exitOnError("Could not load config/xitrum.conf. For an example, see https://github.com/ngocdaothanh/xitrum-new/blob/master/config/xitrum.conf", e)
        null
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Path to the root directory of the current project.
   * If you're familiar with Rails, this is the same as Rails.root.
   * See https://github.com/ngocdaothanh/xitrum/issues/47
   */
  val root = {
    val res = getClass.getClassLoader.getResource("xitrum.conf")
    if (res != null) {
      val fileName = res.getFile
      // This doesn't work on Windows, because "/" is always used in fileName
      // fileName.replace(File.separator + "config" + File.separator + "xitrum.conf", "")
      fileName.replace("/config/xitrum.conf", "")
    } else {
      // Fallback to current working directory
      System.getProperty("user.dir")
    }
  }

  val baseUrl = xitrum.reverseProxy.map(_.baseUrl).getOrElse("")

  /**
   * @param path with leading "/"
   *
   * Avoids returning path with double "//" prefix. Something like
   * //xitrum/postback/zOIc0v...
   * will cause the browser to send request to http://xitrum/postback/zOIc0v...
   */
  def withBaseUrl(path: String) = {
    if (Config.baseUrl.isEmpty)
      path
    else if (path.isEmpty)
      Config.baseUrl
    else if (path.startsWith("/"))
      Config.baseUrl + path
    else
      Config.baseUrl + "/" + path
  }

  //----------------------------------------------------------------------------

  val sessionStore  = {
    val className = xitrum.session.store
    Class.forName(className).newInstance().asInstanceOf[SessionStore]
  }

  /** akka.actor.ActorSystem("xitrum") */
  val actorSystem = ActorSystem(ACTOR_SYSTEM_NAME)

  /**
   * Use lazy to avoid starting Hazelcast if it is not used.
   * Starting Hazelcast takes several seconds, sometimes we want to work in
   * sbt console mode and don't like this overhead.
   */
  lazy val hazelcastInstance: HazelcastInstance = {
    // http://hazelcast.com/docs/3.0/manual/multi_html/ch12s07.html
    System.setProperty("hazelcast.logging.type", "slf4j")

    if (xitrum.hazelcastMode == HAZELCAST_MODE_CLUSTER_MEMBER) {
      val path = Config.root + File.separator + "config" + File.separator + "hazelcast_cluster_member.xml"
      System.setProperty("hazelcast.config", path)

      // null: load from "hazelcast.config" system property above
      // http://hazelcast.com/docs/3.0/manual/multi_html/ch12.html
      Hazelcast.newHazelcastInstance(null)
    } else {
      val clientConfig = new XmlClientConfigBuilder("hazelcast_java_client.xml").build()
      HazelcastClient.newHazelcastClient(clientConfig)
    }
  }

  //----------------------------------------------------------------------------

  def warnOnDefaultSecureKey() {
    if (xitrum.session.secureKey == DEFAULT_SECURE_KEY)
      logger.warn("*** For security, change secureKey in config/xitrum.conf to your own! ***")
  }

  /**
   * Shutdowns Hazelcast and call System.exit(-1).
   * Once Hazelcast is started, calling System.exit(-1) does not stop
   * the current process!
   */
  def exitOnError(msg: String, e: Throwable) {
    logger.error(msg, e)
    Hazelcast.shutdownAll()
    System.exit(-1)
  }

  //----------------------------------------------------------------------------

  private[this] val ROUTES_CACHE = "routes.cache"

  /**
   * Use lazy to avoid collecting routes if they are not used.
   * Sometimes we want to work in sbt console mode and don't like this overhead.
   */
  lazy val routes = loadRouteCacheFileOrRecollectWithRetry()

  private def loadRouteCacheFileOrRecollectWithRetry(retried: Boolean = false): RouteCollection = {
    try {
      logger.info("Load file " + ROUTES_CACHE + " or recollect routes...")
      val routeCollector = new RouteCollector
      val discoveredAcc = routeCollector.deserializeCacheFileOrRecollect(ROUTES_CACHE)
      RouteCollection.fromSerializable(discoveredAcc)
    } catch {
      case NonFatal(e) =>
        if (retried) {
          Config.exitOnError("Could not collect routes", e)
          throw e
        } else {
          logger.info("Could not load " + ROUTES_CACHE + ", delete and retry...")
          val file = new File(ROUTES_CACHE)
          file.delete()
          loadRouteCacheFileOrRecollectWithRetry(true)
        }
    }
  }
}
