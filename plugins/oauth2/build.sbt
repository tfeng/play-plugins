name := "oauth2-plugin"

Settings.common

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "security-plugin" % version.value,
  "org.springframework.security.oauth" % "spring-security-oauth2" % "2.0.2.RELEASE"
)
