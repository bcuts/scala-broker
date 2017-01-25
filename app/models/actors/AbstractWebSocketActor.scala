package models.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, actorRef2Scala }
import models._
import play.api.cache.CacheApi

/**
 * Base abstract class for DSA WebSocket actors, implements essential lifecycle hooks and basic helper methods
 * for communicating with the WebSocket remote.
 */
abstract class AbstractWebSocketActor(out: ActorRef, val settings: Settings, val connInfo: ConnectionInfo, val cache: CacheApi)
    extends Actor with ActorLogging {

  protected val ownId = s"Link[${connInfo.linkPath}]"

  private var localMsgId = new IntCounter(1)

  /**
   * Sends handshake to the client.
   */
  override def preStart() = {
    cache.set(connInfo.linkPath, self)
    log.info(s"$ownId: initialized, sending 'allowed' to client")
    sendAllowed(settings.Salt)
  }

  /**
   * Cleans up after the actor stops.
   */
  override def postStop() = {
    if (connInfo != null)
      cache.remove(connInfo.linkPath)
    log.info(s"$ownId: stopped")
  }

  /**
   * Handles incoming message from the client.
   */
  def receive = {
    case EmptyMessage =>
      log.debug(s"$ownId: received empty message from WebSocket, ignoring...")
    case PingMessage(msg, ack) =>
      log.debug(s"$ownId: received ping from WebSocket with msg=$msg, acking...")
      sendAck(msg)
  }

  /**
   * Stops the actor and closes the WS connection.
   */
  def close() = self ! PoisonPill

  /**
   * Sends 'allowed' message to the client.
   */
  private def sendAllowed(salt: Int) = send(AllowedMessage(true, salt))

  /**
   * Sends an ACK back to the client.
   */
  protected def sendAck(remoteMsgId: Int) = send(PingMessage(localMsgId.inc, Some(remoteMsgId)))

  /**
   * Sends the response message to the client.
   */
  protected def sendResponse(responses: DSAResponse*) = send(ResponseMessage(localMsgId.inc, None, responses.toList))

  /**
   * Sends the request message back to the client.
   */
  protected def sendRequest(requests: DSARequest*) = send(RequestMessage(localMsgId.inc, None, requests.toList))

  /**
   * Sends a DSAMessage to a WebSocket connection.
   */
  private def send(msg: DSAMessage) = {
    log.debug(s"$ownId: sending $msg to WebSocket")
    out ! msg
  }
}