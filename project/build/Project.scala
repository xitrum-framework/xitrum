import sbt._

class Project(info: ProjectInfo) extends ParentProject(info) {
  val localMavenRepo = "Local Maven Repo" at
    "file://" + Path.userHome + "/.m2/repository"

  val jbossRepo = "JBoss Repo" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  val scalateRepo = "Scalate Repo" at
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"

  lazy val core    = project("core",    "xitrum-core",    new Core(_))
  lazy val squeryl = project("squeryl", "xitrum-squeryl", new Squeryl(_), core)

  class Core(info: ProjectInfo) extends DefaultProject(info) {
    override def libraryDependencies =
      Set(
        "org.slf4j"              % "slf4j-log4j12" % "1.6.1"       % "compile->default",
        "org.jboss.netty"        % "netty"         % "3.2.2.Final" % "compile->default",
        "org.fusesource.scalate" % "scalate-core"  % "1.3"         % "compile->default"
      ) ++ super.libraryDependencies
  }

  class Squeryl(info: ProjectInfo) extends DefaultProject(info) {
    override def libraryDependencies =
      Set(
        "org.squeryl" % "squeryl_2.8.0" % "0.9.4-RC2" % "compile->default",
        "c3p0"        % "c3p0"          % "0.9.1"     % "compile->default"
      ) ++ super.libraryDependencies
  }
}
