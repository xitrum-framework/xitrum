package xitrum.action.view

import java.io.File
import java.util.UUID
import scala.collection.mutable.ArrayBuffer
import org.jboss.netty.handler.codec.http.FileUpload

import xitrum.action.Action
import xitrum.action.annotation.GET
import xitrum.action.env.session.CSRF
import xitrum.handler.updown.XSendfile

ten xau xi qua, co le nen gom lai thanh prefix uploadXXX
=> neu vay thi thong nhat cai fileParams luon, thanh uploadParams

trait AjaxUpload {
  this: Action =>

  /**
   * The upload will be posted back to the current action under fileParam("file").
   *
   * To get the file name set by the browser: fileParam("file").getFileName
   *
   * To file name set by the browser is not always safe for the server, to
   * sanitize: sanitizeUploadFileName(fileParam("file"))
   *
   * To guess MIME: Mime.get(fileName)
   *
   * To save: saveUpload(fileParam("file"), directory, fileName)
   *
   * When the file is an image, to let the user preview the image, without you
   * having to save it to the final destination (DB etc.):
   *
   *   @GET(value="/products/new", first=true)
   *   class ProductNewCreateAction {
   *     override def execute {
   *       renderView(
   *         <form postback="submit" action={urlForThis}>
   *           Product name:<br />
   *           <input type="text" name="name" /><br />
   *
   *           <div id="image"></div>
   *
   *           Product image:<br />
   *           {ajaxUpload}
   *
   *           <input type="submit" name="Create" />
   *         </form>)
   *     }
   *
   *     override def postback {
   *       if (fileParamo("file").isDefined) previewProductImage else createProduct
   *     }
   *
   *     private def previewProductImage {
   *       val x = saveUploadToTempFile(fileParam("file"))
   *       val src = urlFor[AjaxUploadTempFileServer]("x" -> x)
   *
   *       jsRenderUpdate("image",
   *         <input type="hidden" name="tempFile" value={x} />
   *         <img src={src} />)
   *     }
   *
   *     private def createProduct {
   *       val name = param("name")
   *       val tempFile = param("tempFile")
   *
   *       val id = Product.create(name)
   *       val path = System.getProperty("user.dir") + "/public/products/" + id + extension
   *       saveTempFile(...)
   *
   *       redirectTo[ProductIndexAction]
   *     }
   *   }
   *
   *
   *
   * You may also use the above trick to collect files one by one, before submiting
   * the whole form.
   */
  def ajaxUpload = {
    val uuid = UUID.randomUUID.toString
    ArrayBuffer(
      <iframe id={uuid} name={uuid} src={urlForThis} style="display:none; width:1px; height:1px;"></iframe>,
      <form method="post" enctype="multipart/form-data" target={uuid}>
        <input type="file" name="file" />
        <input type="submit" value="Upload" />
      </form>)
  }

  /**
   * Browser (IE) may send the full file path on the machine it runs. This method
   * returns only the file name, not the full file path.
   */
  def sanitizeUploadFileName(fileUpload: FileUpload): String = {
    val fileName = fileUpload.getFilename
    fileName  // TODO
  }

  /** The directory will be created if it does not exist. */
  def saveUpload(fileUpload: FileUpload, directory: String, fileName: String) {

  }

  /**
   * Returns the ecrypted pair (sanitizedFileName, encryptedTempFilePath)
   *
   * To let the browser preview the uploaded file, use the URL:
   *   urlFor[AjaxUploadTempFileServer]("encryptedTempFilePath" -> encryptedTempFilePath)
   *
   * To move the temp file to a destination:
   *   moveTempFile(encryptedTempFilePath, destinationPath)
   *
   * To decrypt to the temp file path:
   *   CSRF.decrypt(actionObject, encryptedTempFilePath)
   */
  def saveUploadToTempFile(fileUpload: FileUpload): String = {
    val sanitizedFileName = sanitizeUploadFileName(fileUpload)

    // Please read the doc of File.createTempFile, or
    // http://www.rgagnon.com/javadetails/java-0484.html
    val (prefix, suffix) = {
      val tokens = sanitizedFileName.split("\\.")
      if (tokens.size == 1) {
        (sanitizedFileName, null)
      } else {
        val suffix = "." + tokens.last
        val prefix = sanitizedFileName.substring(0, sanitizedFileName.length - suffix.length)
        (prefix, suffix)
      }
    }
    val temp = File.createTempFile(prefix, suffix)
    temp.deleteOnExit

    val tempFilePath = temp.getPath
    val encryptedTempFilePath = CSRF.encrypt(this, tempFilePath)

    CSRF.encrypt(this, (sanitizedFileName, encryptedTempFilePath))
  }

  def saveTempFile(encryptedTempFilePath: String, destinationPath: String) {

  }

}

@GET("/xitrum/ajax_uploads/:encryptedTempFilePath")
class AjaxUploadTempFileServer extends Action {
  override def execute {
    val encryptedTempFilePath = param("encryptedTempFilePath")
    val tempFilePath          = CSRF.decrypt(this, encryptedTempFilePath)

    response.setHeader(XSendfile.XSENDFILE_HEADER, tempFilePath)
    respond
  }
}
