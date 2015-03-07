package xitrum.util

// http://groups.google.com/group/scala-user/browse_thread/thread/bb5214c5360b13c3

sealed class DefaultsTo[A, B]

trait LowPriorityDefaultsTo {
  implicit def overrideDefault[A, B] = new DefaultsTo[A, B]
}

object DefaultsTo extends LowPriorityDefaultsTo {
  implicit def default[B] = new DefaultsTo[B, B]
}
