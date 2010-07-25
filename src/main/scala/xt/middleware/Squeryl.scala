package xt.middleware

import scala.collection.mutable.Map

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import org.squeryl.{SessionFactory, Session}
import org.squeryl.adapters.{MySQLAdapter, PostgreSqlAdapter}
import org.squeryl.PrimitiveTypeMode._

import com.mchange.v2.c3p0.ComboPooledDataSource

/**
 * This middleware:
 */
object Squeryl {
  /**
   * @param driver: postgresql, mysql, or oracle
   */
  def wrap(app: App, driver: String) = {
    setupDB(driver)
    new App {
      def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
        inTransaction { app.call(channel, request, response, env) }
      }
    }
  }

  private def setupDB(driver: String) {
    val (driverClassName, adapter) = driver match {
      case "postgresql" => ("org.postgresql.Driver", new PostgreSqlAdapter)
    }

    val cpds = new ComboPooledDataSource
    cpds.setDriverClass(driverClassName)

    val concreteFactory = () =>
      Session.create(
        cpds.getConnection,
        adapter)
    SessionFactory.concreteFactory = Some(concreteFactory)
  }
}
