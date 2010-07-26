package colinh.model

import org.squeryl.PrimitiveTypeMode._
import Schema._

class Article(
    var id:        Int,
    var title:     String,
    var teaser:    String,
    var body:      String,
    var sticky:    Boolean,
    var hits:      Int,
    var createdAt: DateType,
    var updatedAt: DateType,
    var userId:    Int) {
  def this() = this(0, "", "", "", false, 0, null, null, 0)
}

object Article {
  val pageLength = 10

  def all = from(articles)(a => select(a) orderBy(a.updatedAt desc))

  def page(p: Int) = {
    val size = from(articles)(a => compute(count)).toInt
    val numPages1 = size/pageLength
    val numPages2 = if (numPages1*pageLength < size) numPages1 + 1 else numPages1
    val col = all.page(p - 1, pageLength)
    (numPages2, col)
  }

  def first(id: Long): Option[Article] = {
    val q = from(articles)(a => where(a.id === id) select(a))
    if (q.size == 0) None else Some(q.single)
  }
}
