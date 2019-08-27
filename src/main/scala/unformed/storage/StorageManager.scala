package unformed.storage

import java.sql.Timestamp

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import unformed.client.ClientHandler.PrintMessage
import unformed.model.{Candle, Deal}
import unformed.storage.StorageManager._

import scala.concurrent.Future
import scala.concurrent.duration._

class StorageManager extends Actor with LazyLogging {
  private val actorsMap = collection.mutable.Map[String, ActorRef]()
  private val subscribers = collection.mutable.ListBuffer[ActorRef]()
  import context.dispatcher

  override def receive: Receive = {
    case SubscribeMessage(actor) =>
      subscribers.append(actor)
      logger.info(s"User is subscribed : ${actor.path.name}")
      implicit val timeout: Timeout = Timeout(5 seconds)
      Future.traverse(actorsMap.values){ actor =>
        actor.ask(CollectCandlesMessage(10, new Timestamp(System.currentTimeMillis()))).mapTo[Seq[Candle]]
      }.map(_.flatten.map(PrintMessage.apply)).foreach(_.foreach(actor ! _))

    case UnsubscribeMessage(actor) =>
      subscribers.remove(subscribers.indexOf(actor))
      logger.info(s"User is unsubscribed : ${actor.path.name}, subscribers: ${subscribers.map(_.path.name)}")

    case Tick =>
      logger.info(s"Tick")

      implicit val timeout: Timeout = Timeout(5 seconds)
      Future.traverse(actorsMap.values) {actor =>
        actor.ask(Tick).mapTo[Candle]
      }.map(_.foreach { candle =>
        subscribers.foreach(_ ! PrintMessage(candle))
      })

    case msg @ IncomingMessage(deal) =>
      val storageActor = actorsMap.getOrElseUpdate(deal.ticker, context.actorOf(Props[CandleActor]))
      storageActor ! msg
  }
}

object StorageManager {

  case class SubscribeMessage(actor: ActorRef)
  case class UnsubscribeMessage(actor: ActorRef)
  case class IncomingMessage(deal: Deal)
  case class CollectCandlesMessage(duration: Int, timestamp: Timestamp)
  case object Tick

}
