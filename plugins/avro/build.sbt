import me.tfeng.play.plugins._

name := "avro-plugin"

sbtPlugin := true

Settings.common

libraryDependencies += "org.apache.avro" % "avro-ipc" % Versions.avro

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtDustPlugin)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../../project"
