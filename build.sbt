name := "parent"

Settings.common ++ Settings.disablePublishing

lazy val parent =
    project in file(".") aggregate(spring, security, oauth2, oauth2Example, dust, dustExample)

lazy val spring =
    project in file("plugins/spring") enablePlugins(PlayJava)

lazy val security =
    project in file("plugins/security") enablePlugins(PlayJava) dependsOn(spring)

lazy val oauth2 =
    project in file("plugins/oauth2") enablePlugins(PlayJava) dependsOn(security)

lazy val dust =
    project in file("plugins/dust") enablePlugins(PlayJava) dependsOn(spring)

lazy val oauth2Example =
    project in file("examples/oauth2") enablePlugins(PlayJava) dependsOn(oauth2)

lazy val dustExample =
    project in file("examples/dust") enablePlugins(PlayJava) dependsOn(dust)

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
