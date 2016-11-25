name := "dreamhouse-pio"

parallelExecution in Test := false

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.9.6",
  "org.apache.spark" %% "spark-core"    % "1.3.0",
  "org.apache.spark" %% "spark-mllib"   % "1.3.0",
  "org.scalatest"    %% "scalatest"     % "2.2.1" % "test",
  "ai.h2o" % "sparkling-water-core_2.10" % "1.4.8"
)

cancelable in Global := true

mainClass in Compile := Some("ServerApp")

enablePlugins(JavaAppPackaging)
