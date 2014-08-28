import me.tfeng.play.plugins._

name := "mongodb-plugin"

Settings.common

libraryDependencies += "org.mongodb" % "mongo-java-driver" % Versions.mongoDb
