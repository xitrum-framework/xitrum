package xitrum.scope.session

import java.io.Serializable
import scala.collection.mutable.{Map => MMap}

class CookieSession extends Session {
  private var map = MMap[String, Serializable]()

  def decrypt(base64String: String) {
    map = SecureBase64.decrypt(base64String) match {
      case None        => MMap[String, Serializable]()
      case Some(value) =>
        try {
          // See serialize method below
          val immutableMap = value.asInstanceOf[Map[String, Serializable]]
          val ret = MMap[String, Serializable]()
          ret ++= immutableMap
        } catch {
          case _ =>
            // Cannot always deserialize and type casting due to program changes etc.
            MMap[String, Serializable]()
        }
    }
  }

  def encrypt: String = {
    // See deserialize method above
    // Convert to immutable because mutable cannot always be deserialize later!
    val immutableMap = map.toMap
    SecureBase64.encrypt(immutableMap)
  }

  //----------------------------------------------------------------------------

  def update(key: String, value: Any) { map(key) = value.asInstanceOf[Serializable] }

  def get[T](key: String): T = map(key).asInstanceOf[T]

  def isDefinedAt(key: String) = map.isDefinedAt(key)

  def delete(key: String) { map.remove(key) }

  def reset { map.clear }
}
