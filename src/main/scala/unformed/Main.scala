package unformed

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source, Tcp}
import akka.util.ByteString
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import scodec.bits.BitVector
import unformed.client.Server
import unformed.model.Deal
import unformed.storage.StorageManager
import unformed.storage.StorageManager.{IncomingMessage, Tick}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("parent-system")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val host = "127.0.0.1"
  val port = 5555

  val storage = system.actorOf(Props[StorageManager])
  val clientServer = system.actorOf(Props(new Server(storage)))

  val scheduler: QuartzSchedulerExtension = QuartzSchedulerExtension(system)
  scheduler.createSchedule("everyMinute", None, "0 0/1 * ? * * *")
  scheduler.schedule("everyMinute", storage, Tick)

  Source.maybe[ByteString]
    .via(Tcp().outgoingConnection(host, port))
    .map(BitVector.apply)
    .map(value => Deal.codec.decodeValue(value).require)
    .to(Sink.foreach(msg => storage ! IncomingMessage(msg))).run()

}
