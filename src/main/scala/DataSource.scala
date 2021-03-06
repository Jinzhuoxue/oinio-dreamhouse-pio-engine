
import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.Params
import io.prediction.data.store.PEventStore

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors

import grizzled.slf4j.Logger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.parse

case class DataSourceParams(appName: String, eventServerIp: String, eventServerPort: String, accessKey: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, ActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {

    /*val labeledPoints: RDD[LabeledPoint] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "user",
      // only keep entities with these required properties defined
      required = Some(List("plan", "attr0", "attr1", "attr2")))(sc)
      // aggregateProperties() returns RDD pair of
      // entity ID and its aggregated properties
      .map { case (entityId, properties) =>
        try {
          LabeledPoint(properties.get[Double]("plan"),
            Vectors.dense(Array(
              properties.get[Double]("attr0"),
              properties.get[Double]("attr1"),
              properties.get[Double]("attr2")
            ))
          )
        } catch {
          case e: Exception => {
            logger.error(s"Failed to get properties ${properties} of" +
              s" ${entityId}. Exception: ${e}.")
            throw e
          }
        }
      }.cache()*/

    val httpClient = new HttpClient()

    val getFavorites = new GetMethod(dsp.eventServerIp +":" + dsp.eventServerPort + "/events.json?accessKey=" + dsp.accessKey)

    httpClient.executeMethod(getFavorites)

    val json = parse(getFavorites.getResponseBodyAsStream)

    logger.info(json)
    val labeledPoints = for {
      JArray(events) <- json
      JObject(event) <- events
      JField("properties", JObject(properties)) <- event
      JField("plan", JString(plan)) <- properties
      JField("attr0", JString(attr0)) <- properties
      JField("attr1", JString(attr1)) <- properties
      JField("attr2", JString(attr2)) <- properties
    } yield LabeledPoint(plan.toDouble, Vectors.dense(Array(attr0.toDouble, attr1.toDouble, attr2.toDouble)))

    logger.info(labeledPoints)
    val rdd = sc.parallelize(labeledPoints)

    new TrainingData(rdd)
  }
}

class TrainingData(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable
