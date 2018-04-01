name := "burst-exporter"
version := "0.2"
scalaVersion := "2.12.5"

val playWSStandalone = "1.1.6"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % playWSStandalone
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7"
libraryDependencies += "io.prometheus" % "simpleclient" % "0.3.0"
libraryDependencies += "io.prometheus" % "simpleclient_httpserver" % "0.3.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"
