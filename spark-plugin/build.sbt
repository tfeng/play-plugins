import me.tfeng.play.plugins._

name := "spark-plugin"

Settings.common

libraryDependencies ++= Seq(
  "org.apache.avro" % "avro-compiler" % Versions.avro,
  "org.apache.hadoop" % "hadoop-client" % Versions.hadoop,
  "org.apache.spark" % "spark-core_2.10" % Versions.spark
)
