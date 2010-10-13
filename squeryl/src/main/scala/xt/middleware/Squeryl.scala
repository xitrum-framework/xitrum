package xt.middleware

import scala.collection.mutable.Map

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import org.squeryl.{SessionFactory, Session}
import org.squeryl.adapters.{MySQLAdapter, OracleAdapter, PostgreSqlAdapter}
import org.squeryl.PrimitiveTypeMode._

import com.mchange.v2.c3p0.ComboPooledDataSource

/**
 * This middleware:
 */
object Squeryl {
  /**
   * @param driver: postgresql, mysql, or oracle
   */
  def wrap(app: App) = {
    setupDB
    new App {
      def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
        // Do not use inTransaction because we want the session to be unbound:
        // http://groups.google.com/group/squeryl/browse_thread/thread/e7fe581f7f5f61f9
        //
        // The connection will be closed here after the transaction
        transaction {
          //org.squeryl.Session.currentSession.setLogger(s => println(s))
          app.call(channel, request, response, env)
        }
      }
    }
  }

  private def setupDB {
    val cpds        = new ComboPooledDataSource
    val driverClass = cpds.getDriverClass
    val adapter     = guessAdapter(driverClass)

    val concreteFactory = () => Session.create(cpds.getConnection, adapter)
    SessionFactory.concreteFactory = Some(concreteFactory)
  }

  /**
   * driverClass: com.mysql.jdbc.Driver or org.postgresql.Driver etc.
   */
  private def guessAdapter(driverClass: String) = {
    val lower = driverClass.toLowerCase
    if (lower.indexOf("mysql") != -1) {
      new MySQLAdapter
    } else if (lower.indexOf("oracle") != -1) {
      new OracleAdapter
    } else if (lower.indexOf("postgresql") != -1) {
      new PostgreSqlAdapter
    } else null
  }
}
