package xitrum.util

import java.nio.file.{Path, StandardWatchEventKinds, WatchEvent}
import better.files.File
import io.methvin.better.files.RecursiveFileMonitor

object FileMonitor {
  type Event = WatchEvent.Kind[Path]
  type Stop = () => Unit
  type EventHandler = (Event, Path, Stop) => Unit

  // Convenient shortcuts
  val CREATE: Event = StandardWatchEventKinds.ENTRY_CREATE
  val DELETE: Event = StandardWatchEventKinds.ENTRY_DELETE
  val MODIFY: Event = StandardWatchEventKinds.ENTRY_MODIFY

  /**
   * Recursively monitors the path.
   * Symlink is not supported: https://github.com/gmethvin/directory-watcher/issues/30
   *
   * @return Function to stop the file monitor
   */
  def monitor(path: Path)(eventHandler: EventHandler): Stop = {
    val mon = new RecursiveFileMonitor(path) {
      override def onEvent(event: Event, file: File, count: Int): Unit = {
        eventHandler(event, file.path, () => stop())
      }
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    mon.start()

    () => mon.stop()
  }
}
