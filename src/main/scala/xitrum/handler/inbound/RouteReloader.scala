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

    Config.routes = Config.loadRoutes(new ClassFileLoader, true)
    SwaggerJson.reloadFromRoutes()
  }

  private def monitorClassesDir(classesDir: Path) {
    FileMonitor.monitorRecursive(FileMonitor.MODIFY, classesDir, { path =>
      CLASSES_DIRS.synchronized {
        // Do this not only for .class files, because file change events may
        // sometimes be skipped!
        shouldReloadOnNextRequest = true

        // https://github.com/lloydmeta/schwatcher
        // Callbacks that are registered with recursive=true are not
        // persistently-recursive. That is, they do not propagate to new files
        // or folders created/deleted after registration. Currently, the plan is
        // to have developers handle this themselves in the callback functions.
        FileMonitor.unmonitorRecursive(FileMonitor.MODIFY, classesDir)

        monitorClassesDir(classesDir)
      }
    })
  }
}
