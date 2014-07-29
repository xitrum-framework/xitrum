package xitrum

import java.nio.file.Path
import scala.util.Properties
import xitrum.routing.SwaggerJson
import xitrum.util.{ClassFileLoader, FileMonitor}

object DevClassLoader {
  /** .class files this these directories will be reloaded. */
  private val CLASSES_DIRS = sclasner.Discoverer.files.filter(_.isDirectory).map(_.toPath)

  /**
   * true by default. Set to false (typically at your program's boot) if you
   * want to disable class autoreload feature in development mode. This is only
   * neccessary in very special cases if you have problem with autoreload.
   */
  var enabled = true

  /** Regex of names of the classes that shouldn't be reloaded. */
  var ignorePattern = "".r

  // "public" because this can be used by, for example, Scalate template engine
  // (xitrum-scalate) to pickup the latest class loader in development mode
  var classLoader = newClassFileLoader()

  def onReload(hook: (ClassLoader) => Unit) {
    CLASSES_DIRS.synchronized {
      onReloads = onReloads :+ hook
    }
  }

  def removeReloadHook(hook: ClassLoader => Unit) {
    CLASSES_DIRS.synchronized {
      onReloads = onReloads.filterNot(_ == hook)
    }
  }

  def reloadIfNeeded() {
    if (Config.productionMode || !enabled) return

    CLASSES_DIRS.synchronized {
      if (needNewClassLoader) {
        needNewClassLoader = false
        classLoader        = newClassFileLoader()

        // Also reload routes
        Config.routes    = Config.loadRoutes(classLoader)
        SwaggerJson.apis = SwaggerJson.loadApis()

        onReloads.foreach { hook => hook(classLoader) }
      }
    }
  }

  def logInDevMode() {
    if (Config.productionMode) return

    if (!enabled)
      Log.info("Routes and classes reloading is disabled")
    else if (ignorePattern.toString.isEmpty)
      Log.info(s"Routes and classes in directories $CLASSES_DIRS will be reloaded")
    else
      Log.info(s"Routes and classes in directories $CLASSES_DIRS will be reloaded; ignore classes: $ignorePattern")
  }

  def load(className: String) = classLoader.loadClass(className)

  //----------------------------------------------------------------------------

  private var needNewClassLoader = false  // Only reload on new request
  private var lastLogAt          = 0L     // Avoid logging too frequently
  private var onReloads          = Seq.empty[(ClassLoader) => Unit]

  // In development mode, watch the directory "classes". If there's modification,
  // mark that at the next request, a new class loader should be created.
  if (!Config.productionMode && enabled) monitorClassesDirs()

  private def newClassFileLoader() = new ClassFileLoader(CLASSES_DIRS) {
    override def ignorePattern = DevClassLoader.ignorePattern
  }

  private def monitorClassesDirs() {
    CLASSES_DIRS.foreach(monitorClassesDir)
  }

  private def monitorClassesDir(classesDir: Path) {
    FileMonitor.monitorRecursive(FileMonitor.MODIFY, classesDir, { path =>
      CLASSES_DIRS.synchronized {
        if (enabled) {
          // Do this not only for .class files, because file change events may
          // sometimes be skipped!
          needNewClassLoader = true

          // Avoid logging too frequently
          val now = System.currentTimeMillis()
          val dt  = now - lastLogAt
          if (dt > 4000) {
            Log.info(s"$classesDir changed; reload classes and routes on next request")
            lastLogAt = now
          }

          // https://github.com/lloydmeta/schwatcher
          // Callbacks that are registered with recursive=true are not
          // persistently-recursive. That is, they do not propagate to new files
          // or folders created/deleted after registration. Currently, the plan is
          // to have developers handle this themselves in the callback functions.
          FileMonitor.unmonitorRecursive(FileMonitor.MODIFY, classesDir)
          monitorClassesDir(classesDir)
        } else {
          needNewClassLoader = false
          FileMonitor.unmonitorRecursive(FileMonitor.MODIFY, classesDir)
        }
      }
    })
  }
}
