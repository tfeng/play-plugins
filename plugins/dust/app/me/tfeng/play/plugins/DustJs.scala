package me.tfeng.play.plugins

import me.tfeng.sbt.plugins.SbtDust
import sbt.{AutoPlugin, addSbtPlugin, toGroupID}

object DustJs extends AutoPlugin {

  override lazy val projectSettings = settings

  lazy val settings = Seq(
      addSbtPlugin("me.tfeng.play-plugins" % "dust-plugin" % Versions.project)
  ) ++ SbtDust.settings
}
