name := "security-plugin"

version := "0.1.0"

organization := "me.tfeng.play-plugins"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

EclipseKeys.withSource := true

libraryDependencies ++= Seq(
  cache,
  "me.tfeng.play-plugins" % "spring-plugin" % "0.1.0",
  "org.springframework.security" % "spring-security-core" % "3.2.3.RELEASE"
)

pomExtra :=
  <developers>
    <developer>
      <email>tfeng@berkeley.edu</email>
      <name>Thomas Feng</name>
      <url>https://github.com/tfeng</url>
      <id>tfeng</id>
    </developer>
  </developers>
  <url>https://github.com/tfeng/play-plugins</url>
  <scm>
    <url>https://github.com/tfeng/play-plugins</url>
    <connection>scm:git:https://github.com/tfeng/play-plugins.git</connection>
    <developerConnection>scm:git:git@github.com:tfeng/play-plugins.git</developerConnection>
  </scm>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
