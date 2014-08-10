import me.tfeng.play.plugins._

name := "oauth2-plugin-example"

Settings.common ++ Settings.disablePublishing ++ Settings.integrationTest

libraryDependencies ++= Seq(
  javaWs % "test",
  "me.tfeng.play-plugins" % "oauth2-plugin" % version.value,
  "org.springframework" % "spring-test" % Versions.spring % "test"
)
