package xitrum.util

import java.nio.file.{Path, Paths, StandardWatchEventKinds, WatchEvent}

import com.beachape.filemanagement.Messages.{RegisterCallback, UnRegisterCallback}
import com.beachape.filemanagement.MonitorActor
import com.beachape.filemanagement.RegistryTypes.Callback

import xitrum.{Config, Log}

object FileMonitor extends Log {
  val CREATE = StandardWatchEventKinds.ENTRY_CREATE
  val DELETE = StandardWatchEventKinds.ENTRY_DELETE
  val MODIFY = StandardWatchEventKinds.ENTRY_MODIFY

  private val FILE_MONITOR_CONCURRENCY = 5
  private val fileMonitorActor = Config.actorSystem.actorOf(MonitorActor(FILE_MONITOR_CONCURRENCY))

  def monitor(event: WatchEvent.Kind[Path], path: Path, callback: Callback*) {
    callback.foreach { cb =>
      fileMonitorActor ! RegisterCallback(event, false, path, cb)
    }
  }

  def unmonitor(event: WatchEvent.Kind[Path], path: Path) {
    fileMonitorActor ! UnRegisterCallback(event, false, path)
  }

  def monitorRecursive(event: WatchEvent.Kind[Path], path: Path, callback: Callback*) {
    callback.foreach { cb =>
      fileMonitorActor ! RegisterCallback(event, true, path, cb)
    }
  }

  def unmonitorRecursive(event: WatchEvent.Kind[Path], path: Path) {
    fileMonitorActor ! UnRegisterCallback(event, true, path)
  }

  def pathFromString(str: String): Path = Paths.get(str).toAbsolutePath
}
