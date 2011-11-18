package xitrum.action

import xitrum.Action
import xitrum.Cache
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import com.sun.xml.internal.messaging.saaj.util.Base64
import javax.crypto.Mac

/**
 * Manual :
 * digestAuthenticate(realm: String = "Very Secure", is_ha1_password: Boolean = true, nonce_with_cache: Boolean = true,
 * algorithm: String = "MD5")(f: (String) => String)
 *
 * is_ha1_password : true --> f:(username) => ha1
 * 					 false --> f: (username) => password
 *
 * nonce_with_cache : true ---> IMPLEMENTATION 1 : using Cache to validate
 * 					 false --> IMPLEMENTATION 2 : using secret_token , Base64 to validate . Not using Cache
 *
 * algorithm :
 * * Server support 3 algorithms :
 * hash algorithm = MD5 / MD5-sess/ HMACSHA1
 *  MD5(data)
 *  or
 *  MD5-sess = KD(secret_token, data) = MD5(concat(secret-token, ":", data))
 *  or
 *  HMACSHA1(secret,data)
 *
 * Depend on your choice. But MD5-sess & HMACSHA1 are not available because of using
 * default secret_token. //TO DO for get secret_token from client
 *
 * Server suport  Digest Access Authentication based on IETF RCF 2617
 * Please read  http://en.wikipedia.org/wiki/Digest_access_authentication
 * and http://www.ietf.org/rfc/rfc2617.txt for more information
 *
 * Ex :
 * import xitrum.Action
 * class MyAction extends Action {
 * beforeFilters("authenticate") = digestAuthenticate("Realm",true,true,"MD5")(f(username)=>{password})
 * }
 */

/**
 * Digest Access Authentication based on IETF RCF 2617
 * Source : https://github.com/rails/rails/blob/master/actionpack/lib/action_controller/metal/http_authentication.rb
 */
trait DigestAuthentication {
  this: Action =>
  /**
   * time_out of server_nonce
   */
  val seconds_time_out = 300 // should read from config file
  /**
   * Server support 3 algorithms :
   * hash algorithm = MD5 / MD5-sess/ HMACSHA1
   *  MD5(data)
   *  or
   *  MD5-sess = KD(secret_token, data) = MD5(concat(secret-token, ":", data))
   *  or
   *  HMACSHA1(secret,data)
   *
   *  Depend on your choice. But MD5-sess & HMACSHA1 are not available because of using
   * default secret_token. //TO DO for get secret_token from client
   */
  val hash_algorithm = "MD5" // should read from config file 

  /**
   * realm
   * This string should contain at least the name of the host performing the authentication and might additionally indicate
   * the collection of users who might have access.
   * Ex: realm = "registered_users@gotham.news.com".
   * or realm = "Very Secure"
   *
   * f : password procedure . If is_ha1_password then f(username) => ha1
   *                          else f (username) => password
   *
   * nonce_with_cache = true ---> generate/validate nonce by implementation 1  (using cache)
   * 					false ---> generate/validate nonce by implemetation 2  (using secret_token , Base64)
   */
  def digestAuthenticate(realm: String = "Very Secure", is_ha1_password: Boolean = true, nonce_with_cache: Boolean = true,
    algorithm: String = "MD5")(f: (String) => String): () => Boolean = () => {

    getRequest match {
      case None =>
        respondDigest(realm, generateOpaque, nonce_with_cache)
        false
      case Some((username, realm_from_client, nonce, uri, qop, nc, cnonce, response, opaque, method)) =>

        // validate nonce 
        val check_nonce = {
          val nonce_from_client = nonce
          if (nonce_with_cache)
            validateNonce(nonce_from_client) // NONCE IMPLEMENTATION 1
          else validateNonce2(nonce_from_client) // NONCE IMPLEMENTATION 2
        }

        check_nonce match {
          case false =>
            respondDigestWithStale(realm, generateOpaque, nonce_with_cache)
          case true =>
            // validate response digest && realm && opaque 
            val ha1_or_password = f(username)
            (realm == realm_from_client) && (generateOpaque == opaque) && validateDigestResponse(username, is_ha1_password,
              ha1_or_password, realm, nonce, uri, qop, nc, cnonce, response, method, algorithm)
        }
        true
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
  private def getRequest: Option[(String, String, String, String, String, String, String, String, String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)

    if (authorization == null || !authorization.startsWith("Digest ")) {
      None
    } else {
      val digest = authorization.substring(7) // Skip "Digest "
      val array = digest.split(",")
      val username = array.apply(0).split("=").apply(1)
      val realm = array.apply(1).split("=").apply(1)
      val nonce = array.apply(2).split("=").apply(1)
      val uri = array.apply(3).split("=").apply(1)
      val qop = array.apply(4).split("=").apply(1)
      val nc = array.apply(5).split("=").apply(1)
      val cnonce = array.apply(6).split("=").apply(1)
      val response = array.apply(7).split("=").apply(1)
      val opaque = array.apply(8).split("=").apply(1)
      val method = request.getMethod.toString
      Some((username, realm, nonce, uri, qop, nc, cnonce, response, opaque, method))
    }

  }

  /**
   * If algorithm = MD5:
   * HA1 = MD5( "Mufasa:testrealm@host.com:Circle Of Life" )
   * = 939e7578ed9e3c518a452acee763bce9
   * HA2 = MD5( "GET:/dir/index.html" )
   * = 39aff3a2bab6126f332b942af96d3366
   *
   * response : 2 cases :
   * qop = auth :
   * response = MD5(HA1 : nonce:  HA2)
   * with HA1 = MD5(username:realm:pass)
   * and HA2 = MD5(method:uri)
   *
   * qop = auth-int :
   * response = MD5(HA1 : nonce: nc: cnonce: qop : HA2)
   * with HA1 = MD5(username:realm:pass)
   * and HA2 = MD5(method:uri:MD5(entityBody))   with  entityBody=""
   */
  private def validateDigestResponse(username: String, is_ha1_password: Boolean, ha1_or_password: String, realm: String, nonce: String, uri: String, qop: String, nc: String, cnonce: String,
    response: String, method: String, algorithm: String): Boolean = {

    val ha1 = is_ha1_password match {
      case true =>
        ha1_or_password
      case false =>
        val password = ha1_or_password
        hexaXitrumDigest(username + ":" + realm + ":" + password, algorithm)
    }
    val ha2 = hexaXitrumDigest(method + ":" + uri, algorithm)
    val response2 = qop match {
      case "auth" =>
        hexaXitrumDigest(ha1 + ":" + nonce + ":" + ha2, algorithm)
      case "auth-int" =>
        hexaXitrumDigest(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2, algorithm)
    }
    response2 == response
  }

  /**
   * http_digest_authentication_header : realm , qop , algorithm , nonce , opaque , stale
   * Note : hash_algorithm : see above . Now , we use MD5 algorithm
   *        stale : not present : the username and/or password are invalid . Access denied
   */
  private def respondDigest(realm: String, opaque: String, nonce_with_cache: Boolean) {
    val nonce = { if (nonce_with_cache) generateNonce else generateNonce2 }
    val http_digest_authentication_header = "Digest realm=\"" + realm + "\"," +
      "qop=\"auth,auth-int\"," + "algorithm=\"" + hash_algorithm + "\"," + "nonce=\"" + nonce + "\"," +
      "opaque=\"" + opaque + "\""

    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, http_digest_authentication_header)
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    renderText("HTTP Digest: Access denied.")
  }

  /**
   * "401" status code and add stale=TRUE to the authentication header
   * stale =TRUE means : invalid nonce but with a valid digest for that nonce (indicating that the client knows the
   * correct username/password)
   *  => the client should re-send with the new nonce provided
   *  stale = FALSE or stale not present : the username and/or password are invalid
   */
  private def respondDigestWithStale(realm: String, opaque: String, nonce_with_cache: Boolean) {
    val nonce = { if (nonce_with_cache) generateNonce else generateNonce2 }
    val http_digest_authentication_header = "Digest realm=\"" + realm + "\"," +
      "qop=\"auth,auth-int\"," + "algorithm=\"" + hash_algorithm + "\"," + "nonce=\"" + nonce + "\"," +
      "opaque=\"" + opaque + "\"," + "stale=TRUE"
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, http_digest_authentication_header)
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    renderText("Invalid nonce")
  }

  /**
   *  The contents of the nonce are implementation dependent.
   *  The quality of the implementation depends on a good choice.
   *  A nonce might, for example, be constructed as the base 64 encoding of time-stamp
   *  Ex : H(time-stamp ":" ETag ":" private-key)
   *
   *  Here 2 implementations
   */

  // IMPLEMENTATION 1 : using java UUID && restore in Cache with time-out
  // WARNING : If Hazelcast not work , DON'T USE THIS IMPLEMENTATION  
  private def generateNonce: String = {
    val uuid = java.util.UUID.randomUUID.toString
    Cache.putIfAbsentSecond(uuid, uuid, seconds_time_out)
    uuid
  }

  private def validateNonce(nonce_from_client: String): Boolean = {
    Cache.getAs[String](nonce_from_client) match {
      case None => false
      case Some(a) => true
    }
  }

  // IMPLEMENTATION 2 : using nonce = Base64.encode(time_stamp ":" MD5(time-stamp ":" secret_token))
  // Work without Cache
  private def generateNonce2: String = {
    val time = now
    val secret_token = getSecretTokenClient
    val digest = hexaMessageDigest(time + ":" + secret_token, "MD5")
    val bytes = (time + ":" + digest).getBytes
    new String(Base64.encode(bytes))
  }

  private def validateNonce2(nonce_from_client: String): Boolean = {
    val secret_token = getSecretTokenClient
    val t = Integer.parseInt(Base64.base64Decode(nonce_from_client).split(":").first)
    val digest_from_client = Base64.base64Decode(nonce_from_client).split(":").last
    val digest_server = hexaMessageDigest(t + ":" + secret_token, "MD5")
    digest_server == digest_from_client && (t - now).abs <= seconds_time_out
  }

  private def now: Int = {
    (System.currentTimeMillis() / 1000).asInstanceOf[Int]
  }
  /**
   * opaque :(IETF RCF 2617)A string of data, specified by the server, which should be returned
   * by the client unchanged in the Authorization header of subsequent requests with URIs in the same protection space.
   * It is recommended that this string be base64 or hexadecimal data.
   * => Ex : opaque = "c1bff3a8456c2b98101a66980ced2dde"
   *
   * IMPLEMENTATION :
   * opaque generated depends on secret_token that returned by client. 1 client _ 1 opaque
   * If not, using default secret_token --> default opaque
   */
  private def generateOpaque: String = {
    hexaMessageDigest(getSecretTokenClient, "MD5")
  }

  /**
   * @secret_token : should returned by client . If not, using default token
   */
  private def getSecretTokenClient: String = {
    var secret_token = "rtrrgnccvhdadjdjohcdbdjafpoffhfgfjgfusu" // default
    //TODO : get from client 
    secret_token
  }
  /**
   * algorithm = MD5 , MD5-sess, HMAC
   * combine : hexaMessageDigest + hexaHmacSHA1Signature
   */
  private def hexaXitrumDigest(data: String, algorithm: String): String = {
    algorithm match {
      case "MD5" =>
        hexaMessageDigest(data, "MD5")
      case "MD5-sess" =>
        val secret_with_data = getSecretTokenClient + ":" + data
        hexaMessageDigest(secret_with_data, "MD5")
      case "HMACSHA1" =>
        hexaHmacSHA1Signature(data, getSecretTokenClient)
    }
  }
  /**
   * Hash algorithm = MD2, MD5 , SHA1,...
   * return : hexa data
   */
  private def hexaMessageDigest(data: String, algorithmName: String): String = {
    val messageDigest = MessageDigest.getInstance(algorithmName)
    val buffer = data.getBytes
    messageDigest.update(buffer)
    val messageDigestBytes = messageDigest.digest
    convertArrayBytes2Hexa(messageDigestBytes)
  }
  /**
   * Hash with secret_key :HMAC
   * return : hexa data
   */
  private def hexaHmacSHA1Signature(data: String, secret_key: String): String = {
    val secretKey = new SecretKeySpec(secret_key.getBytes("UTF-8"), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1");
    mac.init(secretKey)
    val hmacDataBytes = mac.doFinal(data.getBytes("UTF-8"))
    convertArrayBytes2Hexa(hmacDataBytes)
  }

  private def convertArrayBytes2Hexa(bytes: Array[Byte]): String = {
    var hexa = ""
    for (x <- bytes) {
      val i = x & 0xff
      if (Integer.toHexString(i).length == 1) hexa = hexa + "0"
      hexa = hexa + Integer.toHexString(i)
    }
    hexa
  }
}