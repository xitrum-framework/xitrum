package xt

trait View {
  // The result can be String, XML etc.
  def render(controller: Controller): Any
}
