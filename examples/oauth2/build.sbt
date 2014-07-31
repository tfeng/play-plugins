name := "oauth2-plugin-example"

Settings.common ++ Settings.disablePublishing ++ Settings.integrationTest

libraryDependencies ++= Seq(
  javaWs,
  "me.tfeng.play-plugins" % "oauth2-plugin" % version.value,
  "org.springframework" % "spring-test" % "4.0.3.RELEASE" % "test"
)
