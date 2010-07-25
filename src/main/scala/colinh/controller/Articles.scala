package colinh.controller

import colinh.model.Article

class Articles extends Application {
  def index {
    at("title", "Colinh Home")
    at("articles", Article.all)
    render
  }

  def show {
    val id = param("id").get.toLong
    at("article", Article.first(id))
    render
  }

  def edit {

  }
}
