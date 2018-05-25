import sbt._

object Dependencies {

  object Codacy {
    val pluginsApi = "com.codacy" %% "codacy-plugins-api" % "2.0.2" withSources ()
    val plugins = "codacy" %% "codacy-plugins" % "4.0.0" classifier "assembly"
    val metricsSeed = "com.codacy" %% "codacy-metrics-scala-seed" % "1.0.0-SNAPSHOT"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.6.9"

  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.4.0"

  val specs2Version = "4.2.0"
  val specs2 = "org.specs2" %% "specs2-core" % specs2Version
}