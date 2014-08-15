import me.tfeng.play.plugins._

name := "dust-plugin-example"

Settings.common ++ Settings.disablePublishing ++ Settings.integrationTest

libraryDependencies ++= Seq(
  javaWs % "test",
  "org.springframework" % "spring-test" % Versions.spring % "test"
)

SbtDust.settings
