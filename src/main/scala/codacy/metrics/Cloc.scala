package codacy.metrics

import better.files._
import com.codacy.docker.api.utils.{CommandResult, CommandRunner}
import com.codacy.plugins.api.languages.Language
import com.codacy.plugins.api.metrics.{FileMetrics, MetricsTool}
import com.codacy.plugins.api.{Options, Source}
import play.api.libs.json._

import scala.util.{Success, Try}

final case class ClocFileMetrics(filename: String, linesOfCode: Int, linesOfComments: Int, blankLines: Int)

object Cloc extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[Options.Key, Options.Value]): Try[List[FileMetrics]] = {

    // If a file is empty in the files array given explictly to the tool with the codacyrc,
    // example: files: [ "" ], it prepends the /src part of the path
    // and the tool will execute for the whole folder accidentally...
    // We clean up that erroneous case so that we protect ourselves from potential
    // problems when the tool is called for example with
    // files: [ "README.md", "" ] by some mistake,
    // where we only wanted to process the explicit files, in this case only "README.md"
    val cleanedUpFiles = files.map(_.excl(Source.File("/src")))

    getLinesCount(source.path, cleanedUpFiles).map { metricsSeq =>
      metricsSeq.map { clocFileMetrics =>
        FileMetrics(
          filename = clocFileMetrics.filename,
          loc = Option(clocFileMetrics.linesOfCode),
          cloc = Option(clocFileMetrics.linesOfComments))
      }
    }
  }

  private def getLinesCount(targetDirectory: String, files: Option[Set[Source.File]]): Try[List[ClocFileMetrics]] = {
    val result = commandResult(targetDirectory, files)

    def stripBaseDir(filePath: String) =
      filePath.stripPrefix("./")

    result.map { jsonOpt =>
      jsonOpt.fold(List.empty[ClocFileMetrics]) { json =>
        for {
          metricsMap <- json.asOpt[Map[String, JsValue]].toList
          (file, metrics) <- metricsMap if (targetDirectory / file).exists
          linesOfCode <- (metrics \ "code").asOpt[Int]
          linesOfComments <- (metrics \ "comment").asOpt[Int]
          blankLines <- (metrics \ "blank").asOpt[Int]
        } yield {
          ClocFileMetrics(stripBaseDir(file), linesOfCode, linesOfComments, blankLines)
        }
      }
    }
  }

  private def commandResult(targetDirectory: String, files: Option[Set[Source.File]]): Try[Option[JsValue]] = {
    val commandRes = baseCommand(targetDirectory, files)

    // It's a valid output of the tool to exit with no errors but still have the
    // output as empty. Example: asking the tool to analyze a file that
    // doesn't exist will terminate with 0 and empty output.
    // We shouldn't try to parse empty string since that's not valid json anyway.

    commandRes.flatMap { fullOutput =>
      val finalOutput: String = fullOutput.mkString

      if (finalOutput.nonEmpty) {
        Try(Json.parse(finalOutput)).map(Some(_))
      } else {
        Success(None)
      }
    }
  }

  private def baseCommand(targetDirectory: String, filesOpt: Option[Set[Source.File]]): Try[List[String]] = {
    val targetDir = new java.io.File(targetDirectory)

    val clocTarget = filesOpt.fold {
      Set[String](".")
    } { _.map(_.path) }

    // If the files Set is empty, which is possible by giving files: [] in the codacyrc,
    // then the tool fails since clocTarget expands to "" and the tool itself errors out.
    // Protecting the call to ensure the tool is NOT called when files to process are empty.

    if (clocTarget.isEmpty) {
      Success(List.empty)
    } else {
      val commandResult =
        CommandRunner.exec(List("cloc", "--json", "--by-file", "--skip-uniqueness") ++ clocTarget, Some(targetDir))

      handleCommandResult(commandResult, clocTarget)
    }
  }

  private def handleCommandResult(either: Either[Throwable, CommandResult],
                                  targetFiles: Set[String]): Try[List[String]] = {
    either match {
      case Left(throwable)                    => scala.util.Failure(throwable)
      case Right(CommandResult(0, stdOut, _)) => scala.util.Success(stdOut)
      case Right(CommandResult(exitCode, stdOut, stdErr)) =>
        val toolErrorMessage =
          s"""Cloc exited with code $exitCode
             |  - targeting files: $targetFiles
             |  - stderr: $stdErr
             |  - stdout: $stdOut
             |""".stripMargin
        scala.util.Failure(new Exception(toolErrorMessage))
    }
  }
}
