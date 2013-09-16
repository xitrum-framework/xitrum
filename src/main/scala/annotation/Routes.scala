package xitrum.annotation

import scala.annotation.StaticAnnotation

case class GET   (paths: String*) extends StaticAnnotation
case class POST  (paths: String*) extends StaticAnnotation
case class PUT   (paths: String*) extends StaticAnnotation
case class PATCH (paths: String*) extends StaticAnnotation
case class DELETE(paths: String*) extends StaticAnnotation

case class OPTIONS(paths: String*) extends StaticAnnotation

case class WEBSOCKET(paths: String*) extends StaticAnnotation
case class SOCKJS   (paths: String*) extends StaticAnnotation

class Error404 extends StaticAnnotation
class Error500 extends StaticAnnotation

class First extends StaticAnnotation
class Last  extends StaticAnnotation

class SockJsCookieNeeded extends StaticAnnotation
class SockJsNoWebSocket  extends StaticAnnotation
