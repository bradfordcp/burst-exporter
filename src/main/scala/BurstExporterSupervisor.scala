import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.{Config, ConfigFactory}

object BurstExporterSupervisor {
  def props(): Props = Props(new BurstExporterSupervisor)

  private val conf: Config = ConfigFactory.load()
}

class BurstExporterSupervisor extends Actor with ActorLogging {
  import BurstExporterSupervisor._

  override def preStart(): Unit = log.info("Burst Exporter Application started")
  override def postStop(): Unit = log.info("Burst Exporter Application stopped")

  // No need to handle any messages
  override def receive = Actor.emptyBehavior

  private val network = context.actorOf(BurstNetworkMonitor.props(conf), "burst-network-monitor")
  private val http = context.actorOf(PrometheusHTTPServer.props(conf), "http-server")
  private val account_monitor = context.actorOf(AccountMonitorSupervisor.props(conf), "account-monitor-supervisor")
}
