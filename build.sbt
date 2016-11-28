import AssemblyKeys._

assemblySettings

name := "dreamhouse-pio"

organization := "io.prediction"

parallelExecution in Test := false

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.9.6" % "provided",
  "org.apache.spark" %% "spark-core"    % "1.2.0" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.2.0" % "provided")

cancelable in Global := true

mainClass in Compile := Some("ServerApp")

enablePlugins(JavaAppPackaging)