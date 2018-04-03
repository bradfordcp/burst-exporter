package network_monitor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import io.prometheus.client.Gauge
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.duration._

import util.DurationConverter._

object BurstNetworkMonitor {
  def props(conf: Config): Props = Props(new BurstNetworkMonitor(conf))

  private val peers_total_gauge: Gauge = Gauge.build.name("burst_peers_total").help("Total number of peers").register()
  private val unlocked_accounts_total_gauge: Gauge = Gauge.build.name("burst_unlocked_accounts_total").help("Total number of unlocked accouts").register()
  private val transfers_total_gauge: Gauge = Gauge.build.name("burst_transfers_total").help("Total number of transfers").register()
  private val orders_total_gauge: Gauge = Gauge.build.name("burst_orders_total").help("Total number of orders").register()
  private val transactions_total_gauge: Gauge = Gauge.build.name("burst_transactions_total").help("Total number of transactions").register()
//  private val cumulative_difficulty_gauge: Gauge = Gauge.build.name("burst_cumulative_difficulty").help("Cumulative difficulty").register() # TODO whittle this down a little more
  private val assets_total_gauge: Gauge = Gauge.build.name("burst_assets_total").help("Total number of assets").register()
//  private val effective_balance_nxt_total: Gauge = Gauge.build.name("burst_effective_balance_nxt_total").help("Total effective balance").register()
  private val accounts_total_gauge: Gauge = Gauge.build.name("burst_accounts_total").help("Total number of accounts").register()
  private val blocks_total_gauge: Gauge = Gauge.build.name("burst_blocks_total").help("Total number of blocks").register()
  private val bid_orders_gauge: Gauge = Gauge.build.name("burst_bid_orders_total").help("Total number of bid orders").register()
  private val aliases_total_gauge: Gauge = Gauge.build.name("burst_aliases_total").help("Total number of aliases").register()
  private val trades_total_gauge: Gauge = Gauge.build.name("burst_trades_total").help("Total number of trades").register()
  private val ask_orders_total_gauge: Gauge = Gauge.build.name("burst_ask_orders_total").help("Total number of ask orders").register()

  final case class PollWallet()

  private case object TickKey
}

class BurstNetworkMonitor(conf: Config) extends Actor with ActorLogging with Timers {
  import BurstNetworkMonitor._

  override def preStart(): Unit = log.info("Burst Network Monitor started")
  override def postStop(): Unit = {
    log.info("Burst Network Monitor stopped")
    wsClient.close()
  }

  private val wsClient = {
    val system: ActorSystem = context.system
    val materializer: ActorMaterializer = ActorMaterializer()(context)

    StandaloneAhcWSClient()(materializer)
  }

  timers.startSingleTimer(TickKey, PollWallet, conf.getDuration("burst_exporter.default_poll_interval"))

  override def receive = {
    case PollWallet =>
//      log.info("Received request to pull network state")

      wsClient.url(s"${conf.getString("burst_exporter.burst_network.wallet_url")}/burst?requestType=getState")
        .withRequestTimeout(10 seconds).get()
        .map(r => {
//          log.info(s"Request received: ${r.statusText}")

          if (r.status == 200) {
            val js = Json.parse(r.body)
            peers_total_gauge.set((js \ "numberOfPeers").as[Double])
            unlocked_accounts_total_gauge.set((js \ "numberOfUnlockedAccounts").as[Double])
            transfers_total_gauge.set((js \ "numberOfTransfers").as[Double])
            orders_total_gauge.set((js \ "numberOfOrders").as[Double])
            transactions_total_gauge.set((js \ "numberOfTransactions").as[Double])
            assets_total_gauge.set((js \ "numberOfAssets").as[Double])
            accounts_total_gauge.set((js \ "numberOfAccounts").as[Double])
            blocks_total_gauge.set((js \ "numberOfBlocks").as[Double])
            bid_orders_gauge.set((js \ "numberOfBidOrders").as[Double])
            aliases_total_gauge.set((js \ "numberOfAliases").as[Double])
            trades_total_gauge.set((js \ "numberOfTrades").as[Double])
            ask_orders_total_gauge.set((js \ "numberOfAskOrders").as[Double])

            timers.startSingleTimer(TickKey, PollWallet, conf.getDuration("burst_exporter.default_poll_interval"))
          } else {
            log.error(r.body)
            timers.startSingleTimer(TickKey, PollWallet, conf.getDuration("burst_exporter.fallback_poll_interval"))
          }
        })(context.dispatcher)
        .recover({
          case e: Exception => log.error(e.getMessage)
            throw e
        })(context.dispatcher)
  }
}
