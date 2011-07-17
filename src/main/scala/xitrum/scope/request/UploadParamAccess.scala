package xitrum.scope.request

import scala.collection.JavaConversions
import org.jboss.netty.handler.codec.http.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam

trait UploadParamAccess {
  this: Action =>

  /**
   * Returns a singular element.
   * The filename has been sanitized for insecure character. You don't have to
   * do it again.
   */
  def uploadParam(key: String): FileUpload = {
    if (fileUploadParams.contains(key)) fileUploadParams.apply(key)(0) else throw new MissingParam(key)
  }

  /** See uploadParam. */
  def uploadParamo(key: String): Option[FileUpload] = {
    val values = fileUploadParams.get(key)
    if (values.isEmpty) None else Some((values.get)(0))
  }

  /**
   * Returns a list of elements.
   * The filename has been sanitized for insecure character. You don't have to
   * do it again.
   */
  def uploadParams(key: String): List[FileUpload] = {
    if (fileUploadParams.contains(key))
      fileUploadParams.apply(key)
    else
      throw new MissingParam(key)
  }

  /** See uploadParams. */
  def uploadParamso(key: String): Option[List[FileUpload]] = {
    fileUploadParams.get(key)
  }
}
