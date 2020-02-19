package xitrum.handler.inbound

import java.nio.file.Path
import xitrum.Config
import xitrum.routing.SwaggerJson
import xitrum.util.{ClassFileLoader, FileMonitor}

// In development mode, classes may be reloaded thus routes should also be
// reloaded.
private class RouteReloader {
  private val CLASSES_DIRS = sclasner.Discoverer.containers.filter(_.isDirectory).map(_.toPath)
  CLASSES_DIRS.foreach(monitorClassesDir)

  private var shouldReloadOnNextRequest = false

  def reloadIfShould(): Unit = synchronized {
    if (!shouldReloadOnNextRequest) return
    shouldReloadOnNextRequest = false

    Config.routes = Config.loadRoutes(new ClassFileLoader, quiet = true)
    SwaggerJson.reloadFromRoutes()
  }

  private def monitorClassesDir(classesDir: Path): Unit = {
    FileMonitor.monitor(classesDir) { (_, _, _) =>
      CLASSES_DIRS.synchronized {
        // Do this not only for .class files, because file change events may
        // sometimes be skipped!
        shouldReloadOnNextRequest = true
      }
    }
  }
}
