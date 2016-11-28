/**
  * Created by OinioC02RG on 11/28/16.
  */
object EngineConfig {

  val engineId = sys.env.getOrElse("ENGINE_ID", "default")
  val engineVersion = sys.env.getOrElse("ENGINE_VERSION", "default")
  val engineVariant = "engine.json"
  val engineVariantId = "default"

}
