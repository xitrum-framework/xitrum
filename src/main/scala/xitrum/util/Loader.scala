package xitrum.util

import java.io.{FileInputStream, InputStream}
import java.util.{Arrays, Properties}

import io.netty.util.CharsetUtil.UTF_8

object Loader {
  private[this] val BUFFER_SIZE = 1024

  /** The input stream will be closed by this method after reading. */
  def bytesFromInputStream(is: InputStream): Array[Byte] = {
    if (is == null) return null

    // Shorter version, but not optimized:
    //
    // var ret    = Array.empty[Byte]
    // var buffer = new Array[Byte](BUFFER_SIZE)
    // while (is.available() > 0) {
    //   val bytesRead = is.read(buffer)
    //   ret = ret ++ buffer.take(bytesRead)
    // }

    // "available" does not always return the exact number of bytes left
    if (is.available() > 0) {
      var buffer = new Array[Byte](BUFFER_SIZE)

      // Use null to avoid creating empty Array[Byte] unnecessarily
      var ret: Array[Byte] = null

      while (is.available() > 0) {
        val bytesRead = is.read(buffer)

        if (ret == null) {
          ret = Arrays.copyOf(buffer, bytesRead)
        } else {
          // http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
          val oldLength = ret.length
          ret = Arrays.copyOf(ret, oldLength + bytesRead)
          System.arraycopy(buffer, 0, ret, oldLength, bytesRead)
        }
      }

      is.close()
      ret
    } else {
      is.close()
      Array.empty[Byte]
    }
  }

  //----------------------------------------------------------------------------

  def bytesFromFile(path: String): Array[Byte] = {
    val is = new FileInputStream(path)
    bytesFromInputStream(is)
  }

  /**
   * @param path Relative to one of the elements in classpath, without leading "/"
   */
  def bytesFromClasspath(path: String): Array[Byte] = {
    val is = getClass.getClassLoader.getResourceAsStream(path)
    bytesFromInputStream(is)
  }

  //----------------------------------------------------------------------------

  def stringFromFile(path: String) =
    new String(bytesFromFile(path), UTF_8)

  /**
   * @param path Relative to one of the elements in classpath, without leading "/"
   */
  def stringFromClasspath(path: String) =
    new String(bytesFromClasspath(path), UTF_8)

  //----------------------------------------------------------------------------

  def propertiesFromFile(path: String): Properties = {
    val is  = new FileInputStream(path)
    val ret = new Properties
    ret.load(is)
    is.close()
    ret
  }

  /**
   * @param path Relative to one of the elements in classpath, without leading "/"
   */
  def propertiesFromClasspath(path: String): Properties = {
    // http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2
    val is  = getClass.getClassLoader.getResourceAsStream(path)
    val ret = new Properties
    ret.load(is)
    is.close()
    ret
  }

  //----------------------------------------------------------------------------

  def jsonFromFile[T](path: String)(implicit m: Manifest[T]): T =
    Json.deserialize[T](stringFromFile(path))

  /**
   * @param path Relative to one of the elements in classpath, without leading "/"
   */
  def jsonFromClasspath[T](path: String)(implicit m: Manifest[T]): T =
    Json.deserialize[T](stringFromClasspath(path))
}
