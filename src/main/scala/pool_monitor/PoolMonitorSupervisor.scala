package pool_monitor

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Created by Christopher Bradford on 4/1/18.
  */
object PoolMonitorSupervisor {
  def props(conf: Config) = Props(new PoolMonitorSupervisor(conf))
}

class PoolMonitorSupervisor(conf: Config) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Pool Monitor Supervisor started")
  override def postStop(): Unit = log.info("Pool Monitor Supervisor stopped")

  override def receive = Actor.emptyBehavior

  // Instantiate a child actor for each account
  private val accounts_to_monitor: List[String] = conf.getStringList("burst_exporter.pool_monitoring.accounts").asScala.toList

  accounts_to_monitor
    .map(account => context.actorOf(PoolMonitor.props(conf, account), account))
    .foreach(actor => actor ! PoolMonitor.Connect())
}
