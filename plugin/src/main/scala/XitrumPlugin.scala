import sbt._
import Keys._

object XitrumPlugin extends Plugin {
  // settings must be lazy to avoid null error
  // distNeedsPackageBin must be after distTask
  override lazy val settings = Seq(distTask, distNeedsPackageBin)

  // Task "dist" ---------------------------------------------------------------

  val dist = TaskKey[Unit]("xitrum-dist", "Prepares target/dist directory, ready for production distribution")

  // distTask must be lazy to avoid null error
  lazy val distTask = dist <<=
      (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map {
      (libs,                                   baseDir,       target, scalaVersion) =>

    val distDir = target / "dist"

    // Copy bin directory
    val binDir1 = baseDir / "bin"
    val binDir2 = distDir / "bin"
    IO.copyDirectory(binDir1, binDir2)
    binDir2.listFiles.foreach { _.setExecutable(true) }

    // Copy config directory
    val configDir1 = baseDir / "config"
    val configDir2 = distDir / "config"
    IO.copyDirectory(configDir1, configDir2)

    // Copy public directory
    val publicDir1 = baseDir / "public"
    val publicDir2 = distDir / "public"
    IO.copyDirectory(publicDir1, publicDir2)

    // Copy lib directory
    val libDir = distDir / "lib"

    // Copy dependencies
    libs.foreach { lib => IO.copyFile(lib.data, libDir / lib.data.name) }

    // Copy .jar files are created after running "sbt package"
    val jarDir = new File(target, "scala-" + scalaVersion.replace('-', '.'))
    (jarDir * "*.jar").get.foreach { file => IO.copyFile(file, libDir / file.name) }
  }

  val distNeedsPackageBin = dist <<= dist.dependsOn(packageBin in Compile)
}
