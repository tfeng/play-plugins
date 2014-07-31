name := "security-plugin"

Settings.common

libraryDependencies ++= Seq(
  cache,
  "me.tfeng.play-plugins" % "spring-plugin" % version.value,
  "org.springframework.security" % "spring-security-core" % "3.2.3.RELEASE"
)
