name := "burst-exporter"
version := "0.5"
scalaVersion := "2.12.5"

val akka = "2.5.11"
val playWSStandalone = "1.1.6"
val playJSON = "2.6.7"
val typesafeConfig = "1.3.2"
val prometheus = "0.3.0"
val javaWebsocket = "1.3.8"
val slf4j = "1.7.25"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akka,
  "com.typesafe.akka" %% "akka-testkit" % akka % Test,

  "com.typesafe.play" %% "play-ahc-ws-standalone" % playWSStandalone,
  "com.typesafe.play" %% "play-json" % playJSON,

  "com.typesafe" % "config" % typesafeConfig,

  "io.prometheus" % "simpleclient" % prometheus,
  "io.prometheus" % "simpleclient_httpserver" % prometheus,

  "org.java-websocket" % "Java-WebSocket" % javaWebsocket,

  "org.slf4j" % "slf4j-simple" % slf4j
)
