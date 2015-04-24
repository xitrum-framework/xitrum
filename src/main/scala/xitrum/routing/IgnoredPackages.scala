package xitrum.routing

private object IgnoredPackages {
  /**
   * @param relPath "/" separated path to the .class file that may contain
   * Xitrum routes
   *
   * @return true for those (java/..., javax/... etc.) that should be ignored
   * because they obviously don't contain Xitrum routes, to speed up the
   * scanning of routes
   */
  def isIgnored(relPath: String) = {
    // Standard Java and Scala
    relPath.startsWith("java/")  ||
    relPath.startsWith("javafx/") ||
    relPath.startsWith("javax/") ||
    relPath.startsWith("scala/") ||
    relPath.startsWith("sun/")   ||
    relPath.startsWith("com/sun/") ||
    // Others
    relPath.startsWith("akka/") ||
    relPath.startsWith("ch/qos/logback") ||
    relPath.startsWith("com/beachape/filemanagement") ||
    relPath.startsWith("com/codahale/metrics") ||
    relPath.startsWith("com/esotericsoftware/kryo/") ||
    relPath.startsWith("com/esotericsoftware/reflectasm/") ||
    relPath.startsWith("com/fasterxml/jackson/") ||
    relPath.startsWith("com/google/") ||
    relPath.startsWith("com/thoughtworks/paranamer/") ||
    relPath.startsWith("com/twitter/") ||
    relPath.startsWith("com/typesafe/") ||
    relPath.startsWith("glokka/") ||
    relPath.startsWith("io/netty/") ||
    relPath.startsWith("javassist/") ||
    relPath.startsWith("nl/grons/metrics/") ||
    relPath.startsWith("org/aopalliance/") ||
    relPath.startsWith("org/apache/") ||
    relPath.startsWith("org/codehaus/commons/") ||
    relPath.startsWith("org/codehaus/janino/") ||
    relPath.startsWith("org/cyberneko/html/") ||
    relPath.startsWith("org/fusesource/hawtjni/") ||
    relPath.startsWith("org/fusesource/leveldbjni/") ||
    relPath.startsWith("org/fusesource/scalamd/") ||
    relPath.startsWith("org/fusesource/scalate/") ||
    relPath.startsWith("org/jboss/") ||
    relPath.startsWith("org/json4s/") ||
    relPath.startsWith("org/iq80/leveldb") ||
    relPath.startsWith("org/mozilla/") ||
    relPath.startsWith("org/objectweb/asm/") ||
    relPath.startsWith("org/objenesis/") ||
    relPath.startsWith("org/openid4java/") ||
    relPath.startsWith("org/slf4j/") ||
    relPath.startsWith("org/slf4s/") ||
    relPath.startsWith("org/w3c/") ||
    relPath.startsWith("org/xml/") ||
    relPath.startsWith("org/uncommons/maths/") ||
    relPath.startsWith("rx/")
  }
}
