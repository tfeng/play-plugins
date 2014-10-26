import me.tfeng.play.plugins._

name := "kafka-plugin"

Settings.common

libraryDependencies += "org.apache.kafka" % "kafka_2.10" % Versions.kafka exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri")
