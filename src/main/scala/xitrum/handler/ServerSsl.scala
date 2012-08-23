package xitrum.handler

import java.io.FileInputStream

import java.security.KeyStore
import java.security.Security

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

import org.jboss.netty.handler.ssl.SslHandler

import xitrum.Config

object ServerSsl {
  // Handler cannot be shared
  def handler() = {
    val ret = new SslHandler(engine)
    ret.setCloseOnSSLException(true)  // https://netty.io/Blog/Netty+355Final+released
    ret
  }

  //----------------------------------------------------------------------------

  private[this] val PROTOCOL  = "TLS"
  private[this] val ALGORITHM = "SunX509"

  // Context can be created only once
  private lazy val context: SSLContext = {
    val ks = KeyStore.getInstance("JKS")
    val is = new FileInputStream(Config.config.keystore.path)
    ks.load(is, Config.config.keystore.password.toCharArray)
    is.close()

    // Set up key manager factory to use our key store
    val kmf = KeyManagerFactory.getInstance(ALGORITHM)
    kmf.init(ks, Config.config.keystore.certificatePassword.toCharArray)

    // Initialize the SSLContext to work with our key managers.
    val ret = SSLContext.getInstance(PROTOCOL)
    ret.init(kmf.getKeyManagers, null, null)
    ret
  }

  // Engine must be recreated everytime
  private def engine: SSLEngine = {
    val ret = context.createSSLEngine()
    ret.setUseClientMode(false)
    ret
  }
}
