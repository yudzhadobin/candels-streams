package unformed.client

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}

class Server(storage: ActorRef) extends Actor {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8080))

  def receive: PartialFunction[Any, Unit] = {
    case b @ Bound(_) => context.parent ! b
    case CommandFailed(_: Bind) => context.stop(self)
    case c @ Connected(_, _) =>
      val connection = sender()
      val handler = context.actorOf(Props(new ClientHandler(storage, connection)))
      connection ! Register(handler)
      handler.tell(c, connection)

  }

}