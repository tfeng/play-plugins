import me.tfeng.play.plugins._

name := "mongodb-plugin"

Settings.common

libraryDependencies ++= Seq(
  "org.apache.avro" % "avro-ipc" % Versions.avro,
  "org.mongodb" % "mongo-java-driver" % Versions.mongoDb
)
