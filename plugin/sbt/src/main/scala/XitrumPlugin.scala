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
      (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map {
      (libs,                                   baseDir,       target, scalaVersion) =>

    val packageDir = target / "xitrum"
    packageDir.mkdirs

    // Copy bin directory
    val binDir1 = baseDir    / "bin"
    val binDir2 = packageDir / "bin"
    IO.copyDirectory(binDir1, binDir2)
    binDir2.listFiles.foreach { _.setExecutable(true) }

    // Copy config directory
    val configDir1 = baseDir    / "config"
    val configDir2 = packageDir / "config"
    IO.copyDirectory(configDir1, configDir2)

    // Copy static directory
    val staticDir1 = baseDir    / "static"
    val staticDir2 = packageDir / "static"
    IO.copyDirectory(staticDir1, staticDir2)

    // Copy lib directory
    val libDir = packageDir / "lib"

    // Copy dependencies
    libs.foreach { lib =>
      val file = lib.data

      // Prevent copying directories in classpath, e.g. "config" directory
      if (!file.isDirectory) IO.copyFile(file, libDir / file.name)
    }

    // Copy .jar files are created after running "sbt package"
    val jarDir = new File(target, "scala-" + scalaVersion.replace('-', '.'))
    (jarDir * "*.jar").get.foreach { file => IO.copyFile(file, libDir / file.name) }

    println("Please see target/xitrum directory")
  }

  val xitrumPackageNeedsPackageBin = xitrumPackageKey <<= xitrumPackageKey.dependsOn(packageBin in Compile)
}
