package pool_monitor

import java.net.URI

import akka.actor.{ActorLogging, FSM, Props, Timers}
import com.typesafe.config.Config
import io.prometheus.client.Gauge
import org.java_websocket.handshake.ServerHandshake
import util.DurationConverter._
import play.api.libs.json._

import scala.concurrent.duration._

/**
  * Monitors a single BURST account participating as part of a pool. The pool's URL is extracted from the configuration
  * with the account being supplied during instantiation
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

  // Messages accepted by the Actor
  final case class Connect()
  final case class OnOpen(handshake: ServerHandshake)
  final case class OnClose(code: Int, reason: String, remote: Boolean)
  final case class OnMessage(message: String)
  final case class OnError(ex: Exception)
  private case class PoolUpdate(
                                 last_confirmed_deadline: Long,
                                 effective_capacity: Double,
                                 historical_share: Double,
                                 last_active_block_height: Long,
                                 nConf: Int,
                                 name: String,
                                 pending: Long)

  // States the FSM can be in
  sealed trait State
  case object Disconnected extends State
  case object Connected extends State

  // State data that may be passed as part of a state change
  sealed trait Data
  case object Uninitialized extends Data
  final case class ActiveClient(ws_client: PoolMonitorWSClient) extends Data

  // Object to track set timers
  case object TickKey
}

class PoolMonitor(conf: Config, account: String) extends FSM[PoolMonitor.State, PoolMonitor.Data]
  with ActorLogging with Timers {

  import PoolMonitor._

  private val pool_url: String = conf.getString("burst_exporter.pool_monitoring.pool_url")
  private val fallback_poll_interval: FiniteDuration = conf.getDuration("burst_exporter.fallback_poll_interval")

  override def preStart(): Unit = log.info("Pool Monitor started")

  startWith(Disconnected, Uninitialized)

  when(Disconnected) {
    case Event(Connect(), Uninitialized) =>
      log.info(s"Opening connection to: $pool_url")
      val ws_client = new PoolMonitorWSClient(URI.create(pool_url), self)
      ws_client.connect()

      stay using ActiveClient(ws_client)

    case Event(OnOpen(_), ActiveClient(ws_client)) =>
      log.info(s"Connection opened: $pool_url")
      ws_client.send(account)

      goto(Connected) using ActiveClient(ws_client)
  }

  when(Connected) {
    case Event(OnMessage(message), ActiveClient(ws_client)) =>
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

      stay using ActiveClient(ws_client)

    case Event(OnClose(_, reason, _), ActiveClient(_)) =>
      log.info(s"Connection closed: $reason")

      timers.startSingleTimer(TickKey, Connect, fallback_poll_interval)

      goto (Disconnected) using Uninitialized

    case Event(OnError(ex), ActiveClient(_)) =>
      log.error(ex.getCause, ex.getMessage)
      throw ex
  }

  whenUnhandled {
    case Event(PoolUpdate(last_confirmed_deadline, effective_capacity, historical_share, last_active_block_height, nConf, name, pending), state) =>
      last_confirmed_deadline_gauge.labels(account, name).set(last_confirmed_deadline)
      effective_capacity_gauge.labels(account, name).set(effective_capacity)
      historical_share_gauge.labels(account, name).set(historical_share)
      last_active_block_height_gauge.labels(account, name).set(last_active_block_height)
      valid_deadlines_last_360_gauge.labels(account, name).set(nConf)
      pending_balance_nqt_gauge.labels(account, name).set(pending)

      stay using state

    case Event(event, state) ⇒
      log.error("received unhandled request {} in state {}/{}", event, stateName, state)
      stay using state
  }
}
