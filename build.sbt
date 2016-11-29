import AssemblyKeys._

assemblySettings

name := "dreamhouse-pio"

parallelExecution in Test := false

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

organization := "org.apache.predictionio"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.10.0-incubating" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.3.0",
  "org.apache.spark" %% "spark-mllib"   % "1.3.0",
  "org.scalatest"    %% "scalatest"     % "2.2.1" % "test"
)

cancelable in Global := true

mainClass in Compile := Some("ServerApp")

enablePlugins(JavaAppPackaging)
