import me.tfeng.play.plugins._

name := "avro-d2-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.apache.zookeeper" % "zookeeper" % Versions.zookeeper
)

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
