package xitrum.action.env

import scala.collection.JavaConversions
import org.jboss.netty.handler.codec.http.FileUpload

import xitrum.action.Action
import xitrum.action.exception.MissingParam

trait FileParamAccess {
  this: Action =>

  /**
   * Returns a singular element.
   */
  def fileParam(key: String): FileUpload = {
    if (fileParams.containsKey(key)) fileParams.get(key).get(0) else throw new MissingParam(key)
  }

  def fileParamo(key: String): Option[FileUpload] = {
    val values = fileParams.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def fileParams(key: String): List[FileUpload] = {
    if (fileParams.containsKey(key))
      JavaConversions.asScalaBuffer[FileUpload](fileParams.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def fileParamso(key: String): Option[List[FileUpload]] = {
    val values = fileParams.get(key)
    if (values == null) None else Some(JavaConversions.asScalaBuffer[FileUpload](values).toList)
  }
}
