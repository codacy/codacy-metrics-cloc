organization := "com.codacy"
scalaVersion := "2.13.6"
name := "codacy-metrics-cloc"
libraryDependencies ++= Seq("com.codacy" %% "codacy-metrics-scala-seed" % "0.3.1")
enablePlugins(JavaAppPackaging)
