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
  private val PROTOCOL  = "TLS"
  private val ALGORITHM = "SunX509"

  val protocols: Array[String] = {
    val context = SSLContext.getInstance(PROTOCOL)
    context.init(null, null, null)

    val engine = context.createSSLEngine()

    // Choose the sensible default list of protocols.
    // Disable SSLv3 to avoid POODLE vulnerability.
    val supportedProtocols = engine.getSupportedProtocols
    var ret = addIfSupported(
      supportedProtocols,
      "TLSv1.2", "TLSv1.1", "TLSv1"
    )
    if (ret.isEmpty) engine.getEnabledProtocols else ret
  }

  val ciphers: Array[String] = {
    val context = SSLContext.getInstance(PROTOCOL)
    context.init(null, null, null)

    val engine = context.createSSLEngine()

    // Choose the sensible default list of cipher suites.
    val supportedCiphers = engine.getSupportedCipherSuites
    var ret = addIfSupported(
      supportedCiphers,
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",  // Since JDK 8
      "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
      "TLS_RSA_WITH_AES_128_GCM_SHA256",  // Since JDK 8
      "SSL_RSA_WITH_RC4_128_SHA",
      "SSL_RSA_WITH_RC4_128_MD5",
      "TLS_RSA_WITH_AES_128_CBC_SHA",
      "TLS_RSA_WITH_AES_256_CBC_SHA",
      "SSL_RSA_WITH_DES_CBC_SHA"
    )
    if (ret.isEmpty) engine.getEnabledCipherSuites else ret
  }

  // Context can be created only once
  private val context: SSLContext = {
    val ks = KeyStore.getInstance("JKS")
    val is = new FileInputStream(Config.xitrum.keystore.path)
    ks.load(is, Config.xitrum.keystore.password.toCharArray)
    is.close()

    // Set up key manager factory to use our key store
    val kmf = KeyManagerFactory.getInstance(ALGORITHM)
    kmf.init(ks, Config.xitrum.keystore.certificatePassword.toCharArray)

    // Initialize the SSLContext to work with our key managers.
    val ret = SSLContext.getInstance(PROTOCOL)
    ret.init(kmf.getKeyManagers, null, null)
    ret
  }

  //----------------------------------------------------------------------------

  // Handler cannot be shared
  def handler() = {
    val ret = new SslHandler(engine())
    ret.setCloseOnSSLException(true)
    ret.setIssueHandshake(true)
    ret
  }

  // Engine must be recreated everytime
  private def engine(): SSLEngine = {
    val ret = context.createSSLEngine()
    ret.setUseClientMode(false)
    ret.setEnabledProtocols(protocols)
    ret.setEnabledCipherSuites(ciphers)
    ret
  }

  private def addIfSupported(supported: Array[String], names: String*): Array[String] = {
    var enabled = Seq[String]()
    for (n <- names) if (supported.contains(n)) enabled = enabled :+ n
    enabled.toArray
  }
}
