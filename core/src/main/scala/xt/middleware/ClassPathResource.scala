package xt.middleware

import scala.collection.mutable.HashSet

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

/**
 * This middleware serves files in the class path.
 *
 * For security, to be served a file xxx must be:
 * 1. Refered to by, for example: http://example.com/classpath/xxx
 * 2. Registered with ClassPathResource.register("xxx")
 */
object ClassPathResource {
  val registered = new HashSet[String]()

  def register(path: String) { registered.add(path) }

  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
      val
val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    }
  }
}
