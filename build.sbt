name := "burst-exporter"
version := "0.4"
scalaVersion := "2.12.5"

val playWSStandalone = "1.1.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test,

  "com.typesafe.play" %% "play-ahc-ws-standalone" % playWSStandalone,
  "com.typesafe.play" %% "play-json" % "2.6.7",

  "com.typesafe" % "config" % "1.3.2",
  "io.prometheus" % "simpleclient" % "0.3.0",
  "io.prometheus" % "simpleclient_httpserver" % "0.3.0",
  "org.java-websocket" % "Java-WebSocket" % "1.3.8",
  "org.slf4j" % "slf4j-simple" % "1.7.25"
)
