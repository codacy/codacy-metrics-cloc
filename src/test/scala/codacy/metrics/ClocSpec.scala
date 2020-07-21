package codacy.metrics

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.metrics.FileMetrics
import org.specs2.mutable.Specification

import scala.util.Success

class ClocSpec extends Specification {

  val scalaFileMetrics = FileMetrics("codacy/metrics/DummyScalaFile.scala", None, Some(5), Some(4))
  val rubyDummyFileMetrics = FileMetrics("codacy/metrics/ruby/dummy/HelloWorld.rb", None, Some(10), Some(0))
  val rubyExampleFileMetrics = FileMetrics("codacy/metrics/ruby/example/HelloWorld.rb", None, Some(10), Some(0))

  val targetDir = "src/test/resources"

  "Cloc" should {
    "get metrics" in {
      "all files within a directory" in {
        val expectedFileMetrics = List(scalaFileMetrics, rubyDummyFileMetrics, rubyExampleFileMetrics)
        val fileMetricsMap =
          Cloc.apply(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap should beLike {
          case Success(elems) => elems should containTheSameElementsAs(expectedFileMetrics)
        }
      }

      "specific files" in {
        val expectedFileMetrics = List(rubyDummyFileMetrics, rubyExampleFileMetrics)

        val fileMetricsMap = Cloc.apply(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(rubyDummyFileMetrics.filename), Source.File(rubyExampleFileMetrics.filename))),
          options = Map.empty)

        fileMetricsMap should beLike {
          case Success(elems) => elems should containTheSameElementsAs(expectedFileMetrics)
        }
      }
    }
  }
}
