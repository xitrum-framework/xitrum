package xitrum.util

import java.nio.file.{Path, StandardWatchEventKinds, WatchEvent}
import java.nio.file.WatchEvent.Modifier
import scala.util.control.NonFatal

import com.beachape.filemanagement.Messages.{RegisterCallback, UnRegisterCallback}
import com.beachape.filemanagement.MonitorActor
import com.beachape.filemanagement.RegistryTypes.Callback
import xitrum.Config

object FileMonitor {
  val CREATE = StandardWatchEventKinds.ENTRY_CREATE
  val DELETE = StandardWatchEventKinds.ENTRY_DELETE
  val MODIFY = StandardWatchEventKinds.ENTRY_MODIFY
  val HIGH   = get_com_sun_nio_file_SensitivityWatchEventModifier_HIGH

  // concurrency = 1 is sufficient for most cases
  private val fileMonitorActor = Config.actorSystem.actorOf(MonitorActor(concurrency = 1))

  def monitor(event: WatchEvent.Kind[Path], path: Path, callbacks: Callback*) {
    callbacks.foreach { cb =>
      fileMonitorActor ! RegisterCallback(event, HIGH, false, path, cb)
    }
  }

  def unmonitor(event: WatchEvent.Kind[Path], path: Path) {
    fileMonitorActor ! UnRegisterCallback(event, false, path)
  }

  def monitorRecursive(event: WatchEvent.Kind[Path], path: Path, callback: Callback*) {
    callback.foreach { cb =>
      fileMonitorActor ! RegisterCallback(event, HIGH, true, path, cb)
    }
  }

  def unmonitorRecursive(event: WatchEvent.Kind[Path], path: Path) {
    fileMonitorActor ! UnRegisterCallback(event, true, path)
  }

  // http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
  // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/com/sun/nio/file/SensitivityWatchEventModifier.java
  private def get_com_sun_nio_file_SensitivityWatchEventModifier_HIGH: Option[Modifier] = {
    try {
      val c = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier")
      val f = c.getField("HIGH")
      val m = f.get(c).asInstanceOf[Modifier]
      Some(m)
    } catch {
      case NonFatal(e) =>
        None
    }
  }
}
