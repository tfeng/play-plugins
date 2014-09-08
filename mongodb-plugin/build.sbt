import me.tfeng.play.plugins._
import me.tfeng.sbt.plugins.SbtAvro.SbtAvroKeys._

name := "mongodb-plugin"

Settings.common

libraryDependencies += "org.mongodb" % "mongo-java-driver" % Versions.mongoDb

SbtAvro.settings

// schemataDirectories += "test/resources"
