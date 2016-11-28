//package org.template.classification

import io.prediction.controller.P2LAlgorithm
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.h2o._

import hex.deeplearning.DeepLearning
import hex.deeplearning.DeepLearningModel
import hex.deeplearning.DeepLearningModel.DeepLearningParameters

import grizzled.slf4j.Logger

case class AlgorithmParams(
  epochs: Int
) extends Params

class DeepLearningAlgorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, PersistentDLModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  override def train(sc: SparkContext, data: PreparedData): PersistentDLModel = {
    val h2oContext = new H2OContext(sc).start()
    import h2oContext._
    val sqlContext = new SQLContext(sc)
    import sqlContext._
    data.dataRDD.registerTempTable("Consumption")
    val df = createDataFrame(data.dataRDD)
    val dlParams = new DeepLearningParameters()
    dlParams._train = df('mean, 'variance, 'use)
    dlParams._response_column = "use"
    dlParams._epochs = ap.epochs

    val dl = new DeepLearning(dlParams)
    val model = dl.trainModel().get()
    new PersistentDLModel(model, h2oContext, sqlContext)
  }

  def predict(model: PersistentDLModel, query: Query): PredictedResult = {
    import model.h2oContext._
    import model.sqlContext._
    val cid = query.circuit
    val inputDataFrame = sql(s"SELECT mean, variance, use FROM Consumption WHERE circuit = ${cid}")
    val prediction = model.dlModel.score(inputDataFrame)('predict)
    val results = toRDD[StringHolder](prediction).map(_.result.getOrElse("None")).collect
    new PredictedResult(results(0))
  }

}
