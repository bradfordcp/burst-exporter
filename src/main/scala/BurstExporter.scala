import akka.actor.ActorSystem

import scala.io.StdIn

/**
  * Entry point which starts the Actor system and top level supervisor
  */
object BurstExporter {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("burst-exporter-system")

    // Create top level supervisor
    val supervisor = system.actorOf(BurstExporterSupervisor.props(), "burst-exporter-supervisor")
  }
}
