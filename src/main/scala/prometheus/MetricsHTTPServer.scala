package prometheus

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config
import io.prometheus.client.exporter.HTTPServer

object MetricsHTTPServer {
  def props(conf: Config): Props = Props(new MetricsHTTPServer(conf))
}

class MetricsHTTPServer(conf: Config) extends Actor with ActorLogging {
  private val http = new HTTPServer(conf.getInt("prometheus.http.port"))

  override def preStart(): Unit = log.info("HTTP Actor started")
  override def postStop(): Unit = {
    log.info("HTTP Actor stopped")
    http.stop()
  }

  override def receive = Actor.emptyBehavior
}
