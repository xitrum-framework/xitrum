package colinh.helper

trait ApplicationHelper {
  def renderPaginationLinks(numPages: Int, currentPage: Int, urlPrefix: String) = {
    val col = (1 to numPages).map { p =>
      if (p == currentPage) {
        <b>{p}</b>
      } else {
        <a href={urlPrefix + p}>{p}</a>
      }
    }

    col.mkString(" ")
  }
}
