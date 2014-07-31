name := "oauth2-plugin-example"

version := "0.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  javaWs,
  "me.tfeng.play-plugins" % "oauth2-plugin" % "0.1.0",
  "org.springframework" % "spring-test" % "4.0.3.RELEASE"
)

EclipseKeys.withSource := true

sbt.Keys.fork in Test := false
