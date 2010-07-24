package colinh.controller

import colinh.model.Article

class Articles extends Application {
  def index {
    at("title", "Colinh Home")
    at("articles", List(new Article, new Article))
    render
  }

  def show {

  }

  def edit {

  }
}
