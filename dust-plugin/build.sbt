import me.tfeng.play.plugins._

name := "dust-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator" % Versions.webJarsLocator,
  "org.webjars" % "dustjs-linkedin" % Versions.dustjs
)

addSbtPlugin("me.tfeng.sbt-plugins" % "dust-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
