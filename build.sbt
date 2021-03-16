import com.typesafe.sbt.packager.docker.{Cmd, DockerAlias}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
organization := "com.codacy"
scalaVersion := "2.13.1"
name := "codacy-metrics-cloc"
libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-metrics-scala-seed" % "0.3.0",
  "org.specs2" %% "specs2-core" % "4.8.0" % Test)

mappings in Universal ++= {
  (resourceDirectory in Compile).map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

dockerBaseImage := "openjdk:8-jre-alpine"
Docker / daemonUser := "docker"
Docker / daemonGroup := "docker"
dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")

val clocVersion = scala.io.Source.fromFile(".cloc-version").mkString.trim

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    List(
      Cmd("RUN", "adduser -u 2004 -D docker"),
      cmd,
      Cmd(
        "RUN",
        s"""apk update &&
           |apk add perl &&
           |apk add bash curl nodejs-npm &&
           |npm install -g npm@5 &&
           |npm install -g cloc@$clocVersion &&
           |rm -rf /tmp/* &&
           |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")),
      Cmd("RUN", "mv /opt/docker/docs /docs"))

  case other => List(other)
}
