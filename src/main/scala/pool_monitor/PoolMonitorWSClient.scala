package pool_monitor

import java.net.URI

import akka.actor.ActorRef
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake

/**
  * Created by Christopher Bradford on 4/2/18.
  */
class PoolMonitorWSClient(server: URI, delegate: ActorRef) extends WebSocketClient(server) {
  override def onOpen(handshakedata: ServerHandshake): Unit = delegate ! PoolMonitor.OnOpen(handshakedata)

  override def onClose(code: Int, reason: String, remote: Boolean): Unit = delegate ! PoolMonitor.OnClose(code, reason, remote)

  override def onMessage(message: String): Unit = delegate ! PoolMonitor.OnMessage(message)

  override def onError(ex: Exception): Unit = delegate ! PoolMonitor.OnError(ex)
}
