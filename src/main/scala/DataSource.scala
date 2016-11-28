//package org.template.classification

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import grizzled.slf4j.Logger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.parse

//case class DataSourceParams(appId: Int) extends Params
case class DataSourceParams(eventServerIp: String, eventServerPort: String, accessKey: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  /*override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("energy"),
      eventNames = Some(List("data")))(sc)
    val dataRDD: RDD[Consumption] = eventsRDD.map {event =>
      event.event match {
        case "data" =>
          val id = event.properties.get[Int]("id")
          val use = try {
            Some(event.properties.get[String]("enduse"))
          } catch {
            case e: Exception => None
          }
          val mean = event.properties.get[Double]("mean")
          val variance = event.properties.get[Double]("variance")
          Consumption(id, mean, variance, use)
        case _ => throw new Exception(s"Unexpected event ${event} is read.")
      }
    }.cache()
    new TrainingData(dataRDD)
  }*/

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
      JField("circuit", JString(circuit)) <- properties
      JField("mean", JDouble(mean)) <- properties
      JField("variance", JDouble(variance)) <- properties
      JField("use", JString(use)) <- properties
    } yield Consumption(circuit, mean, variance, use)

    val rdd = sc.parallelize(favorites)

    new TrainingData(rdd)
  }
}

class TrainingData(
  val dataRDD: RDD[Consumption]
) extends Serializable
