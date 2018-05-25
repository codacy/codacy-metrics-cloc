name := "codacy-metrics-cloc"

version := "0.1"

import sbt.Keys._
import sbt._
name := """codacy-metrics-cloc"""

version := "1.0.0-SNAPSHOT"

val scalaBinaryVersionNumber = "2.12"
val scalaVersionNumber = s"$scalaBinaryVersionNumber.4"

scalaVersion := scalaVersionNumber
scalaVersion in ThisBuild := scalaVersionNumber
scalaBinaryVersion in ThisBuild := scalaBinaryVersionNumber

organization := "com.codacy"

organizationName := "Codacy"

scapegoatVersion in ThisBuild := "1.3.5"

lazy val codacyMetricsCloc = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    inThisBuild(
      List(
        organization := "com.codacy",
        scalaVersion := scalaVersionNumber,
        version := "0.1.0-SNAPSHOT",
        scalacOptions ++= Common.compilerFlags,
        scalacOptions in Test ++= Seq("-Yrangepos"),
        scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"))),
    name := "codacy-analysis-cli",
    // App Dependencies
    libraryDependencies ++= Seq(
      Dependencies.Codacy.plugins,
      Dependencies.Codacy.pluginsApi,
      Dependencies.Codacy.metricsSeed,
      Dependencies.betterFiles,
      Dependencies.playJson
    ),
    // Test Dependencies
    libraryDependencies ++= Seq(Dependencies.specs2).map(_ % Test))
  .settings(Common.dockerSettings: _*)
  .settings(Common.genericSettings: _*)

version in Docker := "1.0"

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)