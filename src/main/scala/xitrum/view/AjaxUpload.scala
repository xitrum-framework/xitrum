package xitrum.view

import java.io.File
import java.util.UUID
import scala.xml.Node
import io.netty.handler.codec.http.FileUpload

import xitrum.Action
import xitrum.annotation.GET
import xitrum.handler.updown.XSendFile
import xitrum.util.SecureBase64

object AjaxUpload {
  def ::(elem: Node)(implicit action: Action) = {
    val uuid = UUID.randomUUID.toString
    <xml:group>
      <iframe id={uuid} name={uuid} src={action.urlForPostbackThis} style="display:none; width:1px; height:1px;"></iframe>
      <form method="post" enctype="multipart/form-data" target={uuid}>
        {elem}
        <input type="submit" value="Upload" />
      </form>
    </xml:group>
  }
}

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
   * having to save it to the final destination:
   *
   * {{{
   * @GET(value="/products/new", first=true)
   * class ProductNewCreateAction {
   *   override def execute {
   *     renderView(
   *       {<form>
   *         Product name:<br />
   *         <input type="text" name="name" /><br />
   *
   *         <span id="image"></span>
   *
   *         Product image:<br />
   *         {<input type="file" name="upload" /> :: AjaxUpload}
   *
   *         <input type="submit" name="Create" />
   *       </form> :: Postback("submit")})
   *   }
   *
   *   override def postback {
   *     if (fileParamo("upload").isDefined) previewProduct else createProduct
   *   }
   *
   *   private def previewProduct {
   *     val encyptedFileName = TempFileServer.saveUpload(uploadParam("file"))
   *     val src              = urlFor[TempFileServer]("encyptedFileName" -> encyptedFileName)
   *
   *     jsRenderHtml(jsById("image"),
   *       <input type="hidden" name="encyptedFileName" value={encyptedFileName} />
   *       <img src={src} />)
   *   }
   *
   *   private def createProduct {
   *     val name = param("name")
   *     val id   = Product.create(name)
   *
   *     val encyptedFileName = param("encyptedFileName")
   *     val path             = Config.root + "/public/products/" + id + extension
   *     TempFileServer.renameTo(encyptedFileName, path)
   *
   *     redirectTo[ProductIndexAction]
   *   }
   * }
   * }}}
   *
   * You may also use the above trick to collect files one by one, before submiting
   * the whole form.
   */
  def ajaxUpload = {
    val uuid = UUID.randomUUID.toString
    <xml:group>
      <iframe id={uuid} name={uuid} src={urlForThis} style="display:none; width:1px; height:1px;"></iframe>
      <form method="post" enctype="multipart/form-data" target={uuid}>
        <input type="file" name="file" />
        <input type="submit" value="Upload" />
      </form>
    </xml:group>
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
      val tokens = sanitizedFileName.split('.')
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

    // We don't use CSRF.encrypt because we only need to encrypt,
    // anti-CSRF is not neccessary

    val encryptedTempFilePath = SecureBase64.encrypt(tempFilePath)
    SecureBase64.encrypt((sanitizedFileName, encryptedTempFilePath))
  }

  def saveTempFile(encryptedTempFilePath: String, destinationPath: String) {

  }
}

@GET("/xitrum/ajax_uploads/:encryptedTempFilePath")
class AjaxUploadTempFileServerAction extends Action {
  override def execute {
    val encryptedTempFilePath = param("encryptedTempFilePath")
    val tempFilePath          = SecureBase64.decrypt(encryptedTempFilePath).toString

    XSendFile.setHeader(response, tempFilePath)
    respond
  }
}
