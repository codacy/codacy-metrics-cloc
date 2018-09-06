package codacy.metrics

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.metrics.FileMetrics
import org.specs2.mutable.Specification

import scala.util.Success

class ClocSpec extends Specification {

  val scalaFileMetrics = FileMetrics("codacy/metrics/DummyScalaFile.scala", None, Some(5), Some(4))
  val rubyFileMetrics = FileMetrics("codacy/metrics/HelloWorld.rb", None, Some(10), Some(0))

  val targetDir = "src/test/resources"

  "Cloc" should {
    "get metrics" in {
      "all files within a directory" in {
        val expectedFileMetrics = List(scalaFileMetrics, rubyFileMetrics)
        val fileMetricsMap =
          Cloc.apply(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap should beLike {
          case Success(elems) => elems should containTheSameElementsAs(expectedFileMetrics)
        }
      }

      "specific files" in {
        val expectedFileMetrics = List(rubyFileMetrics)

        val fileMetricsMap = Cloc.apply(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(rubyFileMetrics.filename))),
          options = Map.empty)

        fileMetricsMap should beLike {
          case Success(elems) => elems should containTheSameElementsAs(expectedFileMetrics)
        }
      }
    }
  }
}
