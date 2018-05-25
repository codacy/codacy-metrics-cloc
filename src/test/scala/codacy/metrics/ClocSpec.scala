package codacy.metrics

import codacy.docker.api.Source
import codacy.docker.api.metrics.FileMetrics
import org.specs2.mutable.Specification

class ClocSpec extends Specification {

  val scalaFileMetrics = FileMetrics("src/test/resources/DummyScalaFile.scala",None, Some(5), Some(4))
  val rubyFileMetrics = FileMetrics("src/test/resources/HelloWorld.rb", None, Some(10), Some(0))

  val targetDir = "src/test/resources"

  "Cloc" should {
    "get metrics" in {
      "all files within a directory" in {
        val expectedFileMetrics = Set(
          scalaFileMetrics,
          rubyFileMetrics
        )
        val fileMetricsMap = Cloc.apply(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap.get.toSet shouldEqual expectedFileMetrics
      }

      "specific files" in {
        val expectedFileMetrics = List(
          rubyFileMetrics
        )

        val fileMetricsMap = Cloc.apply(source = Source.Directory(targetDir), language = None, files = Some(Set(Source.File(rubyFileMetrics.filename))), options = Map.empty)

        fileMetricsMap.get shouldEqual expectedFileMetrics
      }
    }
  }
}
