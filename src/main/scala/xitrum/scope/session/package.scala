package xitrum.scope

import scala.collection.mutable.{Map => MMap}

package object session {
  type Session = MMap[String, Any]
}
