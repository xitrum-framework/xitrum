package colinh.controller

class Errors extends Application {
  def error404 {
    at("title", "Not Found")
    render
  }

  def error500 {
    at("title", "Internal Server Error")
    render
  }
}
