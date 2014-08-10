import me.tfeng.play.plugins._

name := "dust-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.webjars" % "dustjs-linkedin" % Versions.dustjs,
  "me.tfeng.play-plugins" % "spring-plugin" % version.value
)

addSbtPlugin("me.tfeng.sbt-plugins" % "dust-plugin" % Versions.sbtDustPlugin)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../../project"
