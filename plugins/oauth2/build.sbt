import me.tfeng.play.plugins._

name := "oauth2-plugin"

Settings.common

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "security-plugin" % version.value,
  "org.springframework.security.oauth" % "spring-security-oauth2" % Versions.springSecurityOauth
)
