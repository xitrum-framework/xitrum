package xitrum.etag

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import java.io.File
import java.io.FileOutputStream

class EtagTest extends FlatSpec with Matchers {
  
  behavior of "ETag"

  it should "stay same while file is not modified" in {
    val file     = File.createTempFile("etag", "test")
    val filePath = file.getAbsoluteFile().toString()
    file.deleteOnExit()
    
    val result = Etag.forFile(filePath, false)
    
    val etag = result match {
      case Etag.Small(_, etag, _, _) => etag
      case _ => fail()
    }
    
    etag should equal (Etag.forFile(filePath, false).asInstanceOf[Etag.Small].etag)
    
    Thread.sleep(1000)
    
    val stream = new FileOutputStream(file)
    stream.write(1)
    stream.close()
    
    etag should not equal (Etag.forFile(filePath, false).asInstanceOf[Etag.Small].etag)
  }
  
}