package account_monitor

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Supervises and starts all AccountMonitor instances
  */
object AccountMonitorSupervisor {
  def props(conf: Config): Props = Props(new AccountMonitorSupervisor(conf))
}

class AccountMonitorSupervisor(conf: Config) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Account Monitor Supervisor started")
  override def postStop(): Unit = log.info("Account Monitor Supervisor stopped")

  override def receive = Actor.emptyBehavior

  // Instantiate a child actor for each account
  private val accounts_to_monitor: List[String] = conf.getStringList("burst_exporter.account_monitoring.accounts")
    .asScala.toList

  accounts_to_monitor.foreach(account => context.actorOf(AccountMonitor.props(conf, account), account))
}
