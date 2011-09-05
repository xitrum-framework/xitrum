package xitrum.util

import java.io.{FileInputStream, InputStream}
import java.util.Properties

object Loader {
  def bytesFromInputStream(is: InputStream): Array[Byte] = {
    val len   = is.available
    val bytes = new Array[Byte](len)
    var total = 0
    while (total < len) {
      val bytesRead = is.read(bytes, total, len - total)
      total += bytesRead
    }
    is.close
    bytes
  }

  /**
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  def stringFromClasspath(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    val bytes  = bytesFromInputStream(stream)
    new String(bytes, "UTF-8")
  }

  /**
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  def propertiesFromClasspath(path: String): Properties = {
    // http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2
    val stream = getClass.getClassLoader.getResourceAsStream(path)

    val ret = new Properties
    ret.load(stream)
    stream.close
    ret
  }

  def propertiesFromFile(path: String): Properties = {
    val stream = new FileInputStream(path)
    val ret = new Properties
    ret.load(stream)
    stream.close
    ret
  }
}
