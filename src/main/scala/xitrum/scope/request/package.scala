package xitrum.scope

import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.multipart.FileUpload

package object request {
  type Params           = MMap[String, Seq[String]]
  type FileUploadParams = MMap[String, Seq[FileUpload]]
}
