import sbt._

class Project(info: ProjectInfo) extends ParentProject(info) {
  val localMavenRepo = "Local Maven Repo" at
    "file://" + Path.userHome + "/.m2/repository"

  val localIvyRepo = "Local Ivy" at
    "file://" + Path.userHome + "/.ivy2/local"

  val jbossRepo = "JBoss Repo" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  val scalateRepo = "Scalate Repo" at
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"

  lazy val core    = project("core",    "xitrum-core",    new Core(_))
  lazy val squeryl = project("squeryl", "xitrum-squeryl", new Squeryl(_), core)

  class Core(info: ProjectInfo) extends DefaultProject(info) {
    // Use SLF4J. Projects using Xitrum must provide a concrete implentation (Logback etc.).
    override def libraryDependencies =
      Set(
        "org.slf4j"              % "slf4j-api"    % "1.6.0",
        "org.jboss.netty"        % "netty"        % "3.2.2.Final",
        "org.fusesource.scalate" % "scalate-core" % "1.3"
      ) ++ super.libraryDependencies
  }

  class Squeryl(info: ProjectInfo) extends DefaultProject(info) {
    // http://www.mchange.com/projects/c3p0/index.html#configuring_logging
    // Redirect c3p0 log to SLF4J (Logback, see above)
    override def libraryDependencies =
      Set(
        "org.squeryl" % "squeryl_2.8.0"    % "0.9.4-RC2",
        "c3p0"        % "c3p0"             % "0.9.1",
        "org.slf4j"   % "log4j-over-slf4j" % "1.6.0"
      ) ++ super.libraryDependencies
  }
}
