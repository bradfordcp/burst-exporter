package account_monitor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import io.prometheus.client.Gauge
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import util.DurationConverter._

import scala.concurrent.duration._

/**
  * Monitors balances of a specific BURST account address
  */
object AccountMonitor {
  def props(conf: Config, account: String): Props = Props(new AccountMonitor(conf, account))

  private val unconfirmed_balance_nqt: Gauge = Gauge.build
    .name("burst_unconfirmed_balance_nqt").help("Unconfirmed Balance NQT")
    .labelNames("account_rs", "name", "account").register()
  private val guaranteed_balance_nqt: Gauge = Gauge.build
    .name("burst_guaranteed_balance_nqt").help("Guaranteed Balance NQT")
    .labelNames("account_rs", "name", "account").register()
  private val forged_balance_nqt: Gauge = Gauge.build
    .name("burst_forged_balance_nqt").help("Forged Balance NQT")
    .labelNames("account_rs", "name", "account").register()
  private val balance_nqt: Gauge = Gauge.build
    .name("burst_balance_nqt").help("Balance NQT")
    .labelNames("account_rs", "name", "account").register()
  private val effective_balance_burst: Gauge = Gauge.build
    .name("burst_effective_balance_burst").help("Effective Balance BURST")
    .labelNames("account_rs", "name", "account").register()

  // Accepted messages
  final case class PollWallet()

  // Object to track timers
  private case object TickKey
}

class AccountMonitor(conf: Config, account: String) extends Actor with ActorLogging with Timers {
  import AccountMonitor._

  private val wallet_url: String = conf.getString("burst_exporter.account_monitoring.wallet_url")
  private val default_poll_interval: FiniteDuration = conf.getDuration("burst_exporter.default_poll_interval")
  private val fallback_poll_interval: FiniteDuration = conf.getDuration("burst_exporter.fallback_poll_interval")

  override def preStart(): Unit = log.info(s"Account Monitor started: $account")
  override def postStop(): Unit = {
    log.info(s"Account Monitor stopped: $account")
    wsClient.close()
  }

  private val wsClient = {
    val system: ActorSystem = context.system
    val materializer: ActorMaterializer = ActorMaterializer()(context)

    StandaloneAhcWSClient()(materializer)
  }

  timers.startSingleTimer(TickKey, PollWallet, default_poll_interval)

  override def receive: Receive = {
    case PollWallet =>
//      log.info("Received request to pull account state")

      wsClient.url(s"$wallet_url/burst?requestType=getAccount&account=$account")
        .withRequestTimeout(10 seconds).get()
        .map(r => {
//          log.info(s"Request received: ${r.statusText}")

          if (r.status == 200) {
            val js = Json.parse(r.body)

            (js \ "errorCode").asOpt[Int] match {
              case Some(code) => {
                val desc = (js \ "errorDescription").as[String]

                log.error(s"Error code $code: $desc")
                timers.startSingleTimer(TickKey, PollWallet, fallback_poll_interval)
              }
              case None => {
                val account_rs: String = (js \ "accountRS").as[String]
                val name: String = (js \ "name").as[String]
                val account_number: String = (js \ "account").as[String]

                unconfirmed_balance_nqt.labels(account_rs, name, account_number).set((js \ "unconfirmedBalanceNQT").as[String].toLong)
                guaranteed_balance_nqt.labels(account_rs, name, account_number).set((js \ "guaranteedBalanceNQT").as[String].toLong)
                forged_balance_nqt.labels(account_rs, name, account_number).set((js \ "forgedBalanceNQT").as[String].toLong)
                balance_nqt.labels(account_rs, name, account_number).set((js \ "balanceNQT").as[String].toLong)
                effective_balance_burst.labels(account_rs, name, account_number).set((js \ "effectiveBalanceBURST").as[String].toLong)

                timers.startSingleTimer(TickKey, PollWallet, default_poll_interval)
              }
            }
          } else {
            log.error(r.body)
            timers.startSingleTimer(TickKey, PollWallet, fallback_poll_interval)
          }
        })(context.dispatcher)
        .recover({
          case e: Exception => log.error(e.getMessage)
            throw e
        })(context.dispatcher)
  }
}
