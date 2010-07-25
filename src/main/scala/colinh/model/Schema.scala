package colinh.model

object Schema extends org.squeryl.Schema {
  val users    = table[User]("user_")  // By default, the table name is "user", which is a PostgreSQL keyword
  val articles = table[Article]
  val comments = table[Comment]
}
