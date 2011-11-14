package xitrum.action
import xitrum.Action
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import xitrum.Cache
import java.security.MessageDigest

/**
 * Digest access authentication
 * http://en.wikipedia.org/wiki/Digest_access_authentication
 * realm : http://publib.boulder.ibm.com/infocenter/cicsts/v3r2/index.jsp?topic=%2Fcom.ibm.cics.ts.whatsnew.doc%2Fcws_security%2Fdfhe4_realm.html
 * http://www.ietf.org/rfc/rfc2617.txt
 */
trait DigestAuthentication {
  this: Action =>
  /**
   * Ex :   realm = "registered_users@music.mobion.com"
   * 		  opaque = "c1bff3a8456c2b98101a66980ced2dde"
   * see: http://www.ietf.org/rfc/rfc2617.txt
   */

  /**
   * f (username) => password
   */
  def DigestAuthentication(realm: String, opaque: String)(f: (String) => Option[String]): () => Boolean = () => {

    getRequest match {
      case None =>
        respondDigest(realm, opaque)
        false
      case Some(a) =>
        val passwordo = f(a._1)
        val ok = (a._2 == realm) && (a._9 == opaque) && (passwordo.isDefined) && checkDigest(a._1, passwordo.get, a._2, a._3,
          a._4, a._5, a._6, a._7, a._8, a._9, a._10)
        if (ok) true else {
          respondDigestWithStale(realm, opaque)
          false
        }

    }
  }

  /**
   * Authorization: Digest username="Mufasa",
   * realm="testrealm@host.com",
   * nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
   * uri="/dir/index.html",
   * qop=auth,
   * nc=00000001,
   * cnonce="0a4f113b",
   * response="6629fae49393a05397450978507c4ef1",
   * opaque="5ccc069c403ebaf9f0171e9517f40e41"
   */

  //username   realm   nonce    uri     qop      nc     cnonce  respond  opaque method
  def getRequest: Option[(String, String, String, String, String, String, String, String, String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)
    val method = request.getMethod.toString
    if (authorization == null || !authorization.startsWith("Digest ")) {
      None
    } else {
      val digest = authorization.substring(7) // Skip "Digest "
      val array = digest.split(",")
      val username = array.apply(0).split("=").apply(1) //1
      val realm = array.apply(1).split("=").apply(1) //2
      val nonce = array.apply(2).split("=").apply(1) //3 
      val uri = array.apply(3).split("=").apply(1) //4
      val qop = array.apply(4).split("=").apply(1) //5
      val nc = array.apply(5).split("=").apply(1) //6
      val cnonce = array.apply(6).split("=").apply(1) //7
      val respond = array.apply(7).split("=").apply(1) //8
      val opaque = array.apply(8).split("=").apply(1) //9
      Some((username, realm, nonce, uri, qop, nc, cnonce, respond, opaque, method))
    }

  }

  /**
   * HA1 = MD5( "Mufasa:testrealm@host.com:Circle Of Life" )
   * = 939e7578ed9e3c518a452acee763bce9
   *
   * HA2 = MD5( "GET:/dir/index.html" )
   * = 39aff3a2bab6126f332b942af96d3366
   * response : 2 cases :
   * qop = auth :
   * response = MD5(HA1 : nonce:  HA2)
   * with HA1 = MD5(username:realm:pass)
   * and HA2 = MD5(method:uri)
   *
   * qop = auth-int :
   * response = MD5(HA1 : nonce: nc: cnonce: qop : HA2)
   * with HA1 = MD5(username:realm:pass)
   * and HA2 = MD5(method:uri:MD5(entityBody))   ---> entityBody=""
   */
  private def checkDigest(username: String, password: String, realm: String, nonce: String, uri: String, qop: String, nc: String, cnonce: String,
    respond: String, opaque: String, method: String): Boolean = {
    // check realm
    // check nonce
    Cache.getAs[String](nonce) match {
      case None => false // Nonce expired
      case Some(nonce) =>
        val ha1 = calculateSecurityHash(username + ":" + realm + ":" + password, "MD5")
        val ha2 = calculateSecurityHash(method + ":" + uri, "MD5")
        val respond2 = qop match {
          case "auth" =>
            calculateSecurityHash(ha1 + ":" + nonce + ":" + ha2, "MD5")
          case "auth-int" =>
            calculateSecurityHash(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2, "MD5")
        }
        if (respond2 == respond)
          true
        else false

    }
  }

  private def generateNonce: String = {
    val uuid = java.util.UUID.randomUUID.toString
    Cache.putIfAbsentMinute(uuid, uuid, 5)
    uuid
  }

  private def respondDigest(realm: String, opaque: String) {
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Digest realm=\"" + realm + "\","
      + "qop=\"auth,auth-int\"," + "nonce=\"" + generateNonce + "\","
      + "opaque=\"" + opaque + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    renderText("Wrong username or password")
  }

  /**
   * "401" status code and add stale=TRUE to the authentication header, indicating that the client should re-send with
   * the new nonce provided
   */
  private def respondDigestWithStale(realm: String, opaque: String) {
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Digest realm=\"" + realm + "\","
      + "qop=\"auth,auth-int\"," + "nonce=\"" + generateNonce + "\","
      + "opaque=\"" + opaque + "\""
      + "stale=TRUE")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    renderText("Please send the new nonce")
  }

  /**
   * Hash algorithm = MD2, MD5 , SHA1
   *
   */
  private def calculateSecurityHash(stringInput: String, algorithmName: String): String = {
    val messageDigest = MessageDigest.getInstance(algorithmName)
    val hexaMessageEncode = {
      val buffer = stringInput.getBytes
      messageDigest.update(buffer)
      val messageDigestBytes = messageDigest.digest
      var temp = ""
      for (x <- messageDigestBytes) {
        val i = x & 0xff
        if (Integer.toHexString(i).length == 1) temp = temp + "0"
        temp = temp + Integer.toHexString(i)
      }
      temp
    }

    hexaMessageEncode
  }
}