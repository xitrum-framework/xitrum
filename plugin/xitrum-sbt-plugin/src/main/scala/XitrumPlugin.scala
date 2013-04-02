import java.io.FileOutputStream

import sbt._
import Keys._

object XitrumPlugin extends Plugin {
  // Must be lazy to avoid null error
  // xitrumPackageNeedsPackageBin must be after xitrumPackageTask
  override lazy val settings = Seq(xitrumPackageTask, xitrumPackageNeedsPackageBin)

  //----------------------------------------------------------------------------

  val xitrumPackageKey = TaskKey[Unit]("xitrum-package", "Packages to target/xitrum directory, ready for deploying to production server")

  // Must be lazy to avoid null error
  lazy val xitrumPackageTask = xitrumPackageKey <<=
      (externalDependencyClasspath in Runtime, baseDirectory, target,    crossTarget) map {
      (libs,                                   baseDir,       targetDir, jarOutputDir) =>
    try {
      val packageDir = targetDir / "xitrum"
      packageDir.mkdirs

      // Copy bin directory
      val binDir1 = baseDir / "bin"
      if (binDir1 != null) {
        val binDir2 = packageDir / "bin"
        IO.copyDirectory(binDir1, binDir2)
        binDir2.listFiles.foreach { _.setExecutable(true) }
      }

      // Copy config directory
      val configDir1 = baseDir / "config"
      if (configDir1 != null) {
        val configDir2 = packageDir / "config"
        IO.copyDirectory(configDir1, configDir2)
      }

      // Copy public directory
      val publicDir1 = baseDir / "public"
      if (publicDir1 != null) {
        val publicDir2 = packageDir / "public"
        IO.copyDirectory(publicDir1, publicDir2)
      }

      // Copy dependencies to lib directory
      val libDir = packageDir / "lib"
      libs.foreach { lib =>
        val file = lib.data

        // Prevent copying directories in classpath, e.g. "config" directory
        if (!file.isDirectory) IO.copyFile(file, libDir / file.name)
      }

      // Copy .jar files created after running "sbt package" to lib directory
      (jarOutputDir * "*.jar").get.foreach { file => IO.copyFile(file, libDir / file.name) }

      println("Please see target/xitrum directory")
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  val xitrumPackageNeedsPackageBin = xitrumPackageKey <<= xitrumPackageKey.dependsOn(packageBin in Compile)
}
