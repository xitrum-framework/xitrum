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
    if (fileUploadParams.contains(key)) fileUploadParams.apply(key)(0) else throw new MissingParam(key)
  }

  def fileParamo(key: String): Option[FileUpload] = {
    val values = fileUploadParams.get(key)
    if (values.isEmpty) None else Some((values.get)(0))
  }

  /**
   * Returns a list of elements.
   */
  def fileParams(key: String): Array[FileUpload] = {
    if (fileUploadParams.contains(key))
      fileUploadParams.apply(key)
    else
      throw new MissingParam(key)
  }

  def fileParamso(key: String): Option[Array[FileUpload]] = {
    fileUploadParams.get(key)
  }
}
