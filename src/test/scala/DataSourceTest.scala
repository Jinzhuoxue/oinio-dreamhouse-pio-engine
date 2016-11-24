import org.scalatest.{FlatSpec, Matchers}

class DataSourceTest extends FlatSpec with EngineTestSparkContext with Matchers {

  val dataSourceParams = DataSourceParams("http://localhost:8200")

  val dataSource = new DataSource(dataSourceParams)

  "readTraining" should "work" in {
    val favorites = dataSource.readTraining(sc).favorites.collect()
    favorites.length should be (8)
  }

}