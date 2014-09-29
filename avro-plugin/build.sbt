import me.tfeng.play.plugins._

name := "avro-plugin"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.apache.avro" % "avro-ipc" % Versions.avro,
  "org.apache.httpcomponents" % "httpcore" % Versions.httpComponents,
  "org.springframework.security.oauth" % "spring-security-oauth2" % Versions.springSecurityOauth
)

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
