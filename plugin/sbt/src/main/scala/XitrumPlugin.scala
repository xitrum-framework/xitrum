import java.io.FileOutputStream

import sbt._
import Keys._

object XitrumPlugin extends Plugin {
  // Must be lazy to avoid null error
  // xitrumPackageNeedsPackageBin must be after xitrumPackageTask
  override lazy val settings = Seq(xitrumXgettextTask, xitrumPackageTask, xitrumPackageNeedsPackageBin)

  //----------------------------------------------------------------------------

  val xitrumXgettextKey = TaskKey[Unit]("xitrum-xgettext", "Creates i18n.pot")

  lazy val xitrumXgettextTask = xitrumXgettextKey <<= baseDirectory map { baseDir =>
    println("i18n.pot created")
  }

  // ---------------------------------------------------------------------------

  val xitrumPackageKey = TaskKey[Unit]("xitrum-package", "Packages to target/deploy directory, ready for deploying to production server")

  // Must be lazy to avoid null error
  lazy val xitrumPackageTask = xitrumPackageKey <<=
      (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map {
      (libs,                                   baseDir,       target, scalaVersion) =>

    val packageDir = target / "deploy"
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

    println("Please see target/deploy directory")
  }

  val xitrumPackageNeedsPackageBin = xitrumPackageKey <<= xitrumPackageKey.dependsOn(packageBin in Compile)
}
