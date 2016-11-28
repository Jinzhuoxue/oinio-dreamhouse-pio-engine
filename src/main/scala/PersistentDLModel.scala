package org.template.classification

import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.controller.Params

import org.apache.spark.sql.SQLContext
import org.apache.spark.h2o.H2OContext
import org.apache.spark.SparkContext

import hex.deeplearning.DeepLearningModel

class PersistentDLModel (
  val dlModel: DeepLearningModel,
  val h2oContext: H2OContext,
  val sqlContext: SQLContext
) extends IPersistentModel[Params]
  with Serializable {
  
  def save(id: String, params: Params, sc: SparkContext): Boolean = {
    false
  }
}
