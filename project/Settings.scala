package me.tfeng.play.plugins

import sbt._
import sbt.Keys._

object Settings {
  val common: Seq[Setting[_]] = Seq(
    organization := "me.tfeng.play-plugins",
    version := Versions.project,
    scalaVersion := Versions.scala,
    crossPaths := false,
    pomExtra :=
      <developers>
        <developer>
          <email>tfeng@berkeley.edu</email>
          <name>Thomas Feng</name>
          <url>https://github.com/tfeng</url>
          <id>tfeng</id>
        </developer>
      </developers>
      <url>https://github.com/tfeng/play-plugins</url>
      <scm>
        <url>https://github.com/tfeng/play-plugins</url>
        <connection>scm:git:https://github.com/tfeng/play-plugins.git</connection>
        <developerConnection>scm:git:git@github.com:tfeng/play-plugins.git</developerConnection>
      </scm>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
  )

  val disablePublishing: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := (),
    publishM2 := ()
  )
}
