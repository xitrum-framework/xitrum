package colinh.controller

import colinh.model.Article

class Articles extends Application {
  def index {
    //colinh.model.Schema.create

    at("title", "Colinh Home")
    at("articles", Article.all)
    render
  }

  def show {

  }

  def edit {

  }
}
