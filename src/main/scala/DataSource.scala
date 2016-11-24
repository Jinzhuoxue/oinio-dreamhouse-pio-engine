import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import grizzled.slf4j.Logger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class DataSourceParams(dreamHouseWebAppUrl: String) extends Params

class DataSource(val dataSourceParams: DataSourceParams) extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override def readTraining(sc: SparkContext): TrainingData = {

    val httpClient = new HttpClient()

    val getFavorites = new GetMethod(dataSourceParams.dreamHouseWebAppUrl + "/favorite-all")

    httpClient.executeMethod(getFavorites)

    val json = parse(getFavorites.getResponseBodyAsStream)

    val favorites = for {
      JArray(favorites) <- json
      JObject(favorite) <- favorites
      JField("sfid", JString(propertyId)) <- favorite
      JField("favorite__c_user__c", JString(userId)) <- favorite
    } yield Favorite(propertyId, userId)

    val rdd = sc.parallelize(favorites)

    TrainingData(rdd)
  }
}

case class Favorite(propertyId: String, userId: String)

case class TrainingData(favorites: RDD[Favorite])