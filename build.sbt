import me.tfeng.play.plugins._

name := "parent"

Settings.common ++ Settings.disablePublishing

lazy val parent =
    project in file(".") aggregate(spring, security, http, dust, avro, avroD2, oauth2, mongodb)

lazy val spring =
    project in file("spring-plugin") enablePlugins(PlayJava)

lazy val security =
    project in file("security-plugin") enablePlugins(PlayJava) dependsOn(spring)

lazy val http =
    project in file("http-plugin") enablePlugins(PlayJava) dependsOn(spring)

lazy val dust =
    project in file("dust-plugin") enablePlugins(PlayScala) dependsOn(spring)

lazy val avro =
    project in file("avro-plugin") enablePlugins(PlayScala) dependsOn(spring, http)

lazy val avroD2 =
    project in file("avro-d2-plugin") enablePlugins(PlayScala) dependsOn(avro)

lazy val oauth2 =
    project in file("oauth2-plugin") enablePlugins(PlayJava) dependsOn(security, avro)

lazy val mongodb =
    project in file("mongodb-plugin") enablePlugins(PlayJava) dependsOn(avro)
