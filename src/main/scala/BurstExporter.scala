import akka.actor.ActorSystem

import scala.io.StdIn

object BurstExporter {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("burst-exporter-system")

    // Create top level supervisor
    val supervisor = system.actorOf(BurstExporterSupervisor.props(), "burst-exporter-supervisor")
  }
}
