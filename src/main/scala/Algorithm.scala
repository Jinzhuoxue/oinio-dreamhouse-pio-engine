import java.io.File
import java.nio.file.{Files, Paths}

import io.prediction.controller.{PAlgorithm, Params, PersistentModel, PersistentModelLoader}
import io.prediction.data.storage.BiMap
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}

case class AlgorithmParams(rank: Int, numIterations: Int, lambda: Double, tmpDir: String) extends Params {
  def productFeaturesTmpFile = new File(tmpDir, "productFeatures.rdd")
  def userFeaturesTmpFile = new File(tmpDir, "userFeatures.rdd")
  def userStringIntMapTmpFile = new File(tmpDir, "userStringIntMap.rdd")
  def itemStringIntMapTmpFile = new File(tmpDir, "itemStringIntMap.rdd")
}

case class Model(model: MatrixFactorizationModel, userStringIntMap: BiMap[String, Int], itemStringIntMap: BiMap[String, Int]) extends PersistentModel[AlgorithmParams] {
  override def save(id: String, params: AlgorithmParams, sc: SparkContext): Boolean = {
    model.productFeatures.saveAsObjectFile(params.productFeaturesTmpFile.getAbsolutePath)
    model.userFeatures.saveAsObjectFile(params.userFeaturesTmpFile.getAbsolutePath)
    sc.parallelize(Seq(userStringIntMap)).saveAsObjectFile(params.userStringIntMapTmpFile.getAbsolutePath)
    sc.parallelize(Seq(itemStringIntMap)).saveAsObjectFile(params.itemStringIntMapTmpFile.getAbsolutePath)
    true
  }
}

object Model extends PersistentModelLoader[AlgorithmParams, Model] {
  override def apply(id: String, params: AlgorithmParams, sc: Option[SparkContext]): Model = {
    val theSparkContext = sc.get
    val productFeatures = theSparkContext.objectFile[(Int, Array[Double])](params.productFeaturesTmpFile.getAbsolutePath).cache()
    val userFeatures = theSparkContext.objectFile[(Int, Array[Double])](params.userFeaturesTmpFile.getAbsolutePath).cache()
    val model = new MatrixFactorizationModel(params.rank, userFeatures, productFeatures)
    val userStringIntMap = theSparkContext.objectFile[BiMap[String, Int]](params.userStringIntMapTmpFile.getAbsolutePath).first
    val itemStringIntMap = theSparkContext.objectFile[BiMap[String, Int]](params.itemStringIntMapTmpFile.getAbsolutePath).first
    Model(model, userStringIntMap, itemStringIntMap)
  }
}


class Algorithm(val params: AlgorithmParams) extends PAlgorithm[PreparedData, Model, Query, PredictedResult] {

  def train(sc: SparkContext, data: PreparedData): Model = {

    val userStringIntMap = BiMap.stringInt(data.favorites.map(_.userId))
    val itemStringIntMap = BiMap.stringInt(data.favorites.map(_.propertyId))

    val ratings = data.favorites.map { favorite =>
      Rating(userStringIntMap(favorite.userId), itemStringIntMap(favorite.propertyId), 1)
    }.cache()

    val model = ALS.train(ratings, params.rank, params.numIterations, params.lambda)

    Model(model, userStringIntMap, itemStringIntMap)
  }


  def predict(model: Model, query: Query): PredictedResult = {
    val userIdInt = model.userStringIntMap(query.userId)

    val ratings = model.model.recommendProducts(userIdInt, query.numResults)

    val propertyRatings = ratings.map { rating =>
      model.itemStringIntMap.inverse(rating.product) -> rating.rating
    }

    PredictedResult(propertyRatings.toMap)
  }

}
