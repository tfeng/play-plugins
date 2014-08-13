import me.tfeng.play.plugins._

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
    project in file("plugins/dust") enablePlugins(PlayScala) dependsOn(spring)

lazy val oauth2Example =
    project in file("examples/oauth2") enablePlugins(PlayJava) dependsOn(oauth2)

lazy val avroExample =
    project in file("examples/avro") enablePlugins(PlayScala) dependsOn(spring)

lazy val dustExample =
    project in file("examples/dust") enablePlugins(PlayJava) dependsOn(dust)
