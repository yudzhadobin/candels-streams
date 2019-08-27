package unformed.client


import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import io.circe.Encoder
import unformed.model.Candle
import unformed.storage.StorageManager.{SubscribeMessage, UnsubscribeMessage}
import unformed.client.ClientHandler.PrintMessage

import scala.language.postfixOps


class ClientHandler(storage: ActorRef, tcpHandler: ActorRef) extends Actor {
  import akka.io.Tcp._

  override def receive: Receive = {
    case c @ Connected(_, _) =>
      storage ! SubscribeMessage(self)
    case PeerClosed =>
      storage ! UnsubscribeMessage(self)
      context.stop(self)
    case PrintMessage(candle) =>
      val encoder = implicitly[Encoder[Candle]]
      val json = encoder.apply(candle)
      tcpHandler ! Write(ByteString(json.noSpaces + "\n"))
  }

}

object ClientHandler {
  case class PrintMessage(candle: Candle)
}
