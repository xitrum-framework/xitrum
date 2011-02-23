package xitrum.action.env.session

import java.io.Serializable
import scala.collection.mutable.{HashMap => MHashMap}

class CookieSession extends Session {
  private var map = new MHashMap[String, Serializable]

  def deserialize(base64String: String) {
    map = SecureBase64.deserialize(base64String) match {
      case None        => new MHashMap[String, Serializable]
      case Some(value) =>
        try {
          // See serialize method below
          val immutableMap = value.asInstanceOf[Map[String, Serializable]]
          val ret = new MHashMap[String, Serializable]
          ret ++= immutableMap
        } catch {
          case _ =>
            // Cannot always deserialize and type casting due to program changes etc.
            new MHashMap[String, Serializable]
        }
    }
  }

  def serialize: String = {
    // See deserialize method above
    // Convert to immutable because mutable cannot always be deserialize later!
    val immutableMap = map.toMap
    SecureBase64.serialize(immutableMap)
  }

  //----------------------------------------------------------------------------

  def update(key: String, value: Any) { map(key) = value.asInstanceOf[Serializable] }

  def get[T](key: String): T = map(key).asInstanceOf[T]

  def contains(key: String) = map.contains(key)

  def delete(key: String) { map.remove(key) }

  def reset { map.clear }
}
