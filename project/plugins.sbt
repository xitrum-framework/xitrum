// Run sbt eclipse to create Eclipse project file
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.3.0")

// Run sbt gen-idea to create IntelliJ project file
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// Run sbt checkVersions to check updated versions of dependencies
addSbtPlugin("com.sksamuel.sbt-versions" % "sbt-versions" % "0.2.0")

// Run sbt mima-report-binary-issues to check for binary compatibility
// http://www.typesafe.com/community/core-tools/migration-manager
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")
