import me.tfeng.play.plugins._

name := "spring-test"

Settings.common

libraryDependencies ++= Seq(
  "junit" % "junit" % Versions.junit,
  "org.mockito" % "mockito-all" % Versions.mockito,
  "org.springframework" % "spring-test" % Versions.spring
)
