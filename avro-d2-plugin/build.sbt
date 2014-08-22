import me.tfeng.play.plugins._

name := "avro-d2-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "jline" % "jline" % Versions.jline,
  "org.apache.zookeeper" % "zookeeper" % Versions.zookeeper
)

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtPlugins)
