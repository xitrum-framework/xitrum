import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  // Compile options

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // Repos ---------------------------------------------------------------------

  val jboss = "JBoss" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  // For annovention
  val sonatypeSnapshot = "Sonatype Snapshot" at
    "https://oss.sonatype.org/content/repositories/snapshots"

  val scalate = "Scalate" at
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"

  // For Ehcache
  val sf = "SF" at
    "https://oss.sonatype.org/content/repositories/sourceforge-releases"

  override def libraryDependencies =
    Set(
      // Log using SLF4J
      // Projects using Xitrum must provide a concrete implentation (Logback etc.).
      "org.slf4j"              % "slf4j-api"    % "1.6.1" % "provided",

      // Web server
      "org.jboss.netty"        % "netty"        % "3.2.3.Final",

      // For scanning all Controllers to build routes
      "tv.cntt"                % "annovention"  % "1.0-SNAPSHOT",

      // Template engine
      "org.fusesource.scalate" % "scalate-core" % "1.3.2",

      // For sessions and caching files
      "net.sf.ehcache"         % "ehcache-core" % "2.3.0"
    ) ++ super.libraryDependencies

  // Publish -------------------------------------------------------------------

  override def managedStyle = ManagedStyle.Maven

  val publishTo = sonatypeSnapshot

  // ~/.ivy2/.credentials should be:
  // realm=Sonatype Nexus Repository Manager
  // host=oss.sonatype.org
  // user=xxx
  // password=xxx
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
