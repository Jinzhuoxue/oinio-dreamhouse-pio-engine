package org.template.classification

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

class Query(
  val circuit: Int
) extends Serializable

class PredictedResult(
  val enduse: String
) extends Serializable

object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("deeplearning" -> classOf[DeepLearningAlgorithm]),
      classOf[Serving])
  }
}
