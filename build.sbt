import me.tfeng.play.plugins._

name := "parent"

Settings.common ++ Settings.disablePublishing

lazy val parent =
    project in file(".") aggregate(spring, security, oauth2, avro, avroD2, dust)

lazy val spring =
    project in file("spring-plugin") enablePlugins(PlayJava)

lazy val security =
    project in file("security-plugin") enablePlugins(PlayJava) dependsOn(spring)

lazy val oauth2 =
    project in file("oauth2-plugin") enablePlugins(PlayJava) dependsOn(security)

lazy val avro =
    project in file("avro-plugin") enablePlugins(PlayScala) dependsOn(spring)

lazy val avroD2 =
    project in file("avro-d2-plugin") enablePlugins(PlayScala) dependsOn(avro)

lazy val dust =
    project in file("dust-plugin") enablePlugins(PlayScala) dependsOn(spring)
