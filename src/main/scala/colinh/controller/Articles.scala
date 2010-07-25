package colinh.controller

import colinh.model.Article

class Articles extends Application {
  def index {
    at("title", "Home")
    at("articles", Article.all)
    render
  }

  def show {
    val id = param("id").get.toLong
    val article = Article.first(id)
    at("title", article.title)
    at("article", article)
    render
  }

  def edit {

  }
}
