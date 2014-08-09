name := "dust-plugin"

Settings.common

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator" % "0.17",
  "org.webjars" % "dustjs-linkedin" % "2.4.0-1",
  "me.tfeng.play-plugins" % "spring-plugin" % version.value
)
