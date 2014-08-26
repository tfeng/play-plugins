import me.tfeng.play.plugins._

name := "security-plugin"

Settings.common

libraryDependencies ++= Seq(
  cache,
  "org.springframework.security" % "spring-security-core" % Versions.springSecurity
)
