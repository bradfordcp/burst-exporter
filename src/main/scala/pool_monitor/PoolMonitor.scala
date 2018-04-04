package pool_monitor

import java.net.URI

import akka.actor.{Actor, ActorLogging, Props, Timers}
import com.typesafe.config.Config
import io.prometheus.client.Gauge
import org.java_websocket.handshake.ServerHandshake
import org.joda.time.DateTime
import play.api.libs.json._

/**
  * Created by Christopher Bradford on 4/1/18.
  */
object PoolMonitor {
  def props(conf: Config, account: String): Props = Props(new PoolMonitor(conf, account))

  private val last_confirmed_deadline_gauge: Gauge = Gauge.build
    .name("burst_last_confirmed_deadline").help("Last Confirmed Deadline")
    .labelNames("account_rs", "name").register()
  private val effective_capacity_gauge: Gauge = Gauge.build
    .name("burst_effective_capacity").help("Effective Capacity")
    .labelNames("account_rs", "name").register()
  private val historical_share_gauge: Gauge = Gauge.build
    .name("burst_historical_share_percent").help("Historical Share")
    .labelNames("account_rs", "name").register()
  private val last_active_block_height_gauge: Gauge = Gauge.build
    .name("burst_last_active_block_height").help("Last Active Block Height")
    .labelNames("account_rs", "name").register()
  private val valid_deadlines_last_360_gauge: Gauge = Gauge.build
    .name("burst_valid_deadlines_in_last_360").help("Valid Deadlines Last 360 Blocks")
    .labelNames("account_rs", "name").register()
  private val pending_balance_nqt_gauge: Gauge = Gauge.build
    .name("burst_pending_balance_nqt").help("Pending Balance NQT")
    .labelNames("account_rs", "name").register()

  case class OnOpen(handshake: ServerHandshake)
  case class OnClose(code: Int, reason: String, remote: Boolean)
  case class OnMessage(message: String)
  case class OnError(ex: Exception)

  private case class PoolUpdate(last_confirmed_deadline: Long, effective_capacity: Double, historical_share: Double, last_active_block_height: Long, nConf: Int, name: String, pending: Long)
}

class PoolMonitor(conf: Config, account: String) extends Actor with ActorLogging with Timers {
  import PoolMonitor._

  private val pool_url: String = conf.getString("burst_exporter.pool_monitoring.pool_url")
  private val ws_client: PoolMonitorWSClient = new PoolMonitorWSClient(URI.create(pool_url), self)
  ws_client.connect()

  override def preStart(): Unit = log.info(s"Pool Monitor started: $account")
  override def postStop(): Unit = log.info(s"Pool Monitor stopped: $account")

  override def receive: Receive = {
    case PoolUpdate(last_confirmed_deadline, effective_capacity, historical_share, last_active_block_height, nConf, name, pending) => {
      last_confirmed_deadline_gauge.labels(account, name).set(last_confirmed_deadline)
      effective_capacity_gauge.labels(account, name).set(effective_capacity)
      historical_share_gauge.labels(account, name).set(historical_share)
      last_active_block_height_gauge.labels(account, name).set(last_active_block_height)
      valid_deadlines_last_360_gauge.labels(account, name).set(nConf)
      pending_balance_nqt_gauge.labels(account, name).set(pending)
    }
    case OnOpen(serverHandshake) =>{
      log.info(s"Connection opened: $pool_url")
      ws_client.send(account)
    }
    case OnClose(code, reason, remote) => {
      log.info(s"Connection closed: $reason")
      ws_client.connect()
    }
    case OnMessage(message) => {
//      log.info(s"Received message: $message")

      val js = Json.parse(message)

      if (!js.isInstanceOf[JsArray]) {
        if (!js.asInstanceOf[JsObject].keys.contains("scoop") && !js.asInstanceOf[JsObject].keys.contains("subscriptionSuccess"))
          self ! PoolUpdate(
            (js \ "deadline").as[Long],
            (js \ "effectiveCapacity").as[Double],
            (js \ "historicalShare").as[Double],
            (js \ "lastActiveBlockHeight").as[Long],
            (js \ "nConf").as[Int],
            (js \ "name").as[String],
            (js \ "pending").as[Long]
          )
        }
      }

    case OnError(ex) => log.error(ex.getCause, ex.getMessage)
  }
}
