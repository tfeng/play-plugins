import com.typesafe.sbt.pgp._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import sbt._
import Keys._

object Settings {
  val common: Seq[Setting[_]] = Seq(
    organization := "me.tfeng.play-plugins",
    version := "0.1.1-SNAPSHOT",
    // scalaVersion := "2.11.2",
    crossPaths := false
  )

  val disablePublishing: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := (),
    publishM2 := (),
    PgpKeys.publishSigned := (),
    PgpKeys.publishLocalSigned := ()
  )

  val integrationTest = Seq(
    fork in Test := false
  )
}
