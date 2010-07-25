package colinh.model

import org.squeryl.PrimitiveTypeMode._
import Schema._

class Article (
    var id:        Long,
    var title:     String,
    var teaser:    String,
    var body:      String,
    var sticky:    Boolean,
    var hits:      Long,
    var createdAt: DateType,
    var updatedAt: DateType,
    var userId: Long) {
  def this() = this(0, "", "", "", false, 0, null, null, 0)
}

object Article {
  def all: Iterable[Article] = from(articles)(a => select(a))

  def first(id: Long): Option[Article] = {
    val q = from(articles)(a => where(a.id === id) select(a))
    if (q.size == 0) None else Some(q.single)
  }
}
