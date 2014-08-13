package me.tfeng.play.plugins

import me.tfeng.sbt.plugins.SbtDust

import sbt._
import sbt.Keys._

object DustJs extends AutoPlugin {

  override lazy val projectSettings = settings

  lazy val settings = Seq(
      addSbtPlugin("me.tfeng.play-plugins" % "dust-plugin" % Versions.project)
  ) ++ SbtDust.settings
}
