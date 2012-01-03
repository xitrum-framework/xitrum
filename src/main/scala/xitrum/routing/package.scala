package xitrum

package object routing {
  type CompiledPattern = Seq[(String, Boolean)]  // String: token, Boolean: true if the token is constant
}
