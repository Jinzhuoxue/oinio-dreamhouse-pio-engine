import io.prediction.controller.{EmptyActualResult, EmptyEvaluationInfo, PDataSource, Params}
import io.prediction.data.storage.Event
import io.prediction.data.store.PEventStore
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import grizzled.slf4j.Logger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class DataSourceEvalParams(kFold: Int, queryNum: Int)

case class DataSourceParams(eventServerIp: String, eventServerPort: String, accessKey: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
    EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  def getRatings(sc: SparkContext): RDD[Favorite] = {

    val eventsRDD: RDD[Event] = PEventStore.find(
      appName = dsp.eventServerIp,
      entityType = Some("user"),
      eventNames = Some(List("rate", "buy")), // read "rate" and "buy" event
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)

    val favoritesRDD: RDD[Favorite] = eventsRDD.map { event =>
      val favorite = try {
        // entityId and targetEntityId is String
        Favorite(event.properties.get[String]("propertyId"), event.properties.get[String]("userId"))
      } catch {
        case e: Exception => {
          logger.error(s"Cannot convert ${event} to Rating. Exception: ${e}.")
          throw e
        }
      }
      favorite
    }.cache()

    favoritesRDD
  }

  override
  def readTraining(sc: SparkContext): TrainingData = {
    //new TrainingData(getRatings(sc))

    val httpClient = new HttpClient()

    val getFavorites = new GetMethod(dsp.eventServerIp +":" + dsp.eventServerPort + "/events.json?accessKey=" + dsp.accessKey)

    httpClient.executeMethod(getFavorites)

    val json = parse(getFavorites.getResponseBodyAsStream)

    val favorites = for {
      JArray(events) <- json
      JObject(event) <- events
      JField("properties", JObject(properties)) <- event
      JField("propertyId", JString(propertyId)) <- properties
      JField("userId", JString(userId)) <- properties
    } yield Favorite(propertyId, userId)

    val rdd = sc.parallelize(favorites)

    new TrainingData(rdd)
  }
}
case class Favorite(propertyId: String, userId: String)

class TrainingData(
                    val favorites: RDD[Favorite]
                  ) extends Serializable {
  override def toString = {
    s"ratings: [${favorites.count()}] (${favorites.take(2).toList}...)"
  }
}