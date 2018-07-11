package codacy.metrics

import better.files._
import codacy.docker.api.metrics.{FileMetrics, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.Language
import com.codacy.docker.api.utils.CommandRunner
import play.api.libs.json._

import scala.util.Try

final case class ClocFileMetrics(filename: String, linesOfCode: Int, linesOfComments: Int, blankLines: Int)

object Cloc extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language], // Filter by language currently not supported
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    getLinesCount(source.path, files).map { metricsSeq =>
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

    result.map { json =>
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

  private def commandResult(targetDirectory: String, files: Option[Set[Source.File]]): Try[JsValue] = {
    val commandRes = baseCommand(targetDirectory, files)

    commandRes.flatMap { fullOutput =>
      Try(Json.parse(fullOutput.mkString))
    }
  }

  private def baseCommand(targetDirectory: String, filesOpt: Option[Set[Source.File]]): Try[List[String]] = {
    val targetDir = new java.io.File(targetDirectory)

    val clocTarget = filesOpt.fold(Set("."))(_.map(_.path))

    val commandResult = CommandRunner.exec(List("cloc", "--json", "--by-file") ++ clocTarget, Some(targetDir))

    toTry(commandResult).map(_.stdout)
  }

  private def toTry[A](either: Either[Throwable, A]): Try[A] = {
    either match {
      case Left(throwable) => scala.util.Failure(throwable)
      case Right(value)    => scala.util.Success(value)
    }
  }
}
