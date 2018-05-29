package codacy.metrics

import codacy.docker.api.{MetricsConfiguration, Source}
import codacy.docker.api.metrics.{FileMetrics, MetricsTool}
import com.codacy.api.dtos.Language
import com.codacy.docker.api.utils.CommandRunner
import play.api.libs.json._

import scala.util.Try

case class ClocFileMetrics(filename: String, linesOfCode: Int, linesOfComments: Int, blankLines: Int)

object Cloc extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language], // Filter by language currently not supported
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    getLinesCount(source.path, files).map { metricsSeq =>
      metricsSeq.dropRight(1).map { clocFileMetrics =>
        FileMetrics(
          filename = clocFileMetrics.filename,
          loc = Option(clocFileMetrics.linesOfCode),
          cloc = Option(clocFileMetrics.linesOfComments))
      }
    }
  }

  private def getLinesCount(targetDirectory: String, files: Option[Set[Source.File]]): Try[List[ClocFileMetrics]] = {
    val result = commandResult(targetDirectory, files)

    result.map { json =>
      for {
        metricsMap <- json.asOpt[Map[String, JsValue]].toList
        (file, metrics) <- metricsMap
        linesOfCode <- (metrics \ "code").asOpt[Int]
        linesOfComments <- (metrics \ "comment").asOpt[Int]
        blankLines <- (metrics \ "blank").asOpt[Int]
      } yield ClocFileMetrics(file, linesOfCode, linesOfComments, blankLines)
    }
  }

  private def commandResult(targetDirectory: String, files: Option[Set[Source.File]]): Try[JsValue] = {
    val commandRes = baseCommand(targetDirectory, files)

    commandRes.flatMap { fullOutput =>
      Try(Json.parse(fullOutput.mkString))
    }
  }

  private def baseCommand(targetDirectory: String, filesOpt: Option[Set[Source.File]]): Try[List[String]] = {
    val commandResult = filesOpt match {
      case Some(files) =>
        CommandRunner.exec(List("cloc", "--json", "--by-file") ++ files.map(_.path), None)
      case None =>
        CommandRunner.exec(List("cloc", targetDirectory, "--json", "--by-file"), None)
    }
    toTry(commandResult).map(_.stdout)
  }

  private def toTry[A](either: Either[Throwable, A]): Try[A] = {
    either match {
      case Left(throwable) => scala.util.Failure(throwable)
      case Right(value)    => scala.util.Success(value)
    }
  }
}
