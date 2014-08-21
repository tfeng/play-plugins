import me.tfeng.play.plugins._

name := "avro-d2-plugin-example"

Settings.common ++ Settings.disablePublishing ++ Settings.integrationTest

libraryDependencies ++= Seq(
  javaWs % "test",
  "org.springframework" % "spring-test" % Versions.spring % "test"
)

SbtAvro.settings
