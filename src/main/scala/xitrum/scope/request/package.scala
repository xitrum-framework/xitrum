package xitrum.scope

import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.FileUpload

package object request {
  type Params           = MMap[String, List[String]]
  type FileUploadParams = MMap[String, List[FileUpload]]
}
