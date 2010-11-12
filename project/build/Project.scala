import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  // Compile options

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xstrict-warnings",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // Repos ---------------------------------------------------------------------

  val jboss = "JBoss" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  val scalate = "Scalate" at
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"

  val terracotta = "Terracotta" at
    "http://www.terracotta.org/download/reflector/releases"

  val reflections = "Reflections" at "http://reflections.googlecode.com/svn/repo"

  lazy val core    = project("core",    "xitrum-core",    new Core(_))
  lazy val squeryl = project("squeryl", "xitrum-squeryl", new Squeryl(_), core)

  class Core(info: ProjectInfo) extends DefaultProject(info) {
    // Use SLF4J. Projects using Xitrum must provide a concrete implentation (Logback etc.).
    override def libraryDependencies =
      Set(
        // Log
        "org.slf4j"              % "slf4j-api"    % "1.6.1",

        // Web server
        "org.jboss.netty"        % "netty"        % "3.2.3.Final",

        // Template engine
        "org.fusesource.scalate" % "scalate-core" % "1.3.1",

        // For scanning all Controllers to build routes
        "org.reflections"        % "reflections"  % "0.9.5-RC2",  // Uses Javassist

        // For sessions and caching files
        "net.sf.ehcache"    % "ehcache" % "2.2.0",
        "javax.transaction" % "jta"     % "1.1"  // Used by Ehcache
      ) ++ super.libraryDependencies
  }

  class Squeryl(info: ProjectInfo) extends DefaultProject(info) {
    // http://www.mchange.com/projects/c3p0/index.html#configuring_logging
    // Redirect c3p0 log to SLF4J (Logback, see above)
    override def libraryDependencies =
      Set(
        "org.squeryl" % "squeryl_2.8.0"    % "0.9.4-RC2",
        "c3p0"        % "c3p0"             % "0.9.1",
        "org.slf4j"   % "log4j-over-slf4j" % "1.6.1"
      ) ++ super.libraryDependencies
  }
}
