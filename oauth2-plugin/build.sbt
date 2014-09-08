import me.tfeng.play.plugins._

name := "oauth2-plugin"

Settings.common

libraryDependencies += "org.springframework.security.oauth" % "spring-security-oauth2" % Versions.springSecurityOauth

SbtAvro.settings
