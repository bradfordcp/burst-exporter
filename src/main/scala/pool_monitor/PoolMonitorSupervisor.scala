package pool_monitor

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Supervises and starts all PoolMonitor instances
  */
object PoolMonitorSupervisor {
  def props(conf: Config) = Props(new PoolMonitorSupervisor(conf))
}

class PoolMonitorSupervisor(conf: Config) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Pool Monitor Supervisor started")
  override def postStop(): Unit = log.info("Pool Monitor Supervisor stopped")

  override def receive = Actor.emptyBehavior

  // Convert the list of account in the config to a list
  private val accounts_to_monitor: List[String] = conf.getStringList("burst_exporter.pool_monitoring.accounts")
    .asScala.toList

  // Instantiate a monitor for each account and send it the Connect message
  accounts_to_monitor
    .map(account => context.actorOf(PoolMonitor.props(conf, account), account))
    .foreach(actor => actor ! PoolMonitor.Connect())
}
