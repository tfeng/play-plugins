package me.tfeng.play.plugins

import me.tfeng.sbt.plugins.SbtAvro
import sbt.{AutoPlugin, addSbtPlugin, toGroupID}

object Avro extends AutoPlugin {

  override lazy val projectSettings = settings

  lazy val settings = Seq(
      addSbtPlugin("me.tfeng.play-plugins" % "avro-d2-plugin" % Versions.project)
  ) ++ SbtAvro.settings
}
