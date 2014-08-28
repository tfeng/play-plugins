import me.tfeng.play.plugins._

name := "avro-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.apache.avro" % "avro-ipc" % Versions.avro,
  "org.apache.httpcomponents" % "httpcore" % Versions.httpComponents
)

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
