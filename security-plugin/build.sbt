import me.tfeng.play.plugins._

name := "security-plugin"

Settings.common

libraryDependencies ++= Seq(
  cache,
  "me.tfeng.play-plugins" % "spring-plugin" % version.value,
  "org.springframework.security" % "spring-security-core" % Versions.springSecurity
)
