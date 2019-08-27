package unformed.storage

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import unformed.model.{Candle, Deal}
import unformed.storage.StorageManager.{CollectCandlesMessage, IncomingMessage, Tick}


class CandleActor extends Actor with LazyLogging {

  private val history = collection.mutable.ListBuffer[Candle]()
  private var currentCandle: Option[Candle] = None

  override def receive: Receive = {
    case Tick =>
      currentCandle.foreach(history.append(_))
      currentCandle = None
      history.lastOption.foreach(sender() ! _)

    case IncomingMessage(deal) =>
      currentCandle = applyDealToCandle(currentCandle, deal)

    case CollectCandlesMessage(n, time) =>
      val result = history.filter { candle =>
        val duration = time.getTime - candle.timestamp.getTime
        TimeUnit.MILLISECONDS.toMinutes(duration) <= n
      }

      sender() ! Seq(result:_*)
  }

  private def applyDealToCandle(candle: Option[Candle], deal: Deal) = Some {
    candle match {
      case None => Candle(
        deal.ticker,
        cutTimestamp(deal.timestamp),
        deal.price,
        deal.price,
        deal.price,
        deal.price,
        deal.size
      )
      case Some(candle) => Candle(
          candle.ticker,
          candle.timestamp,
          candle.open,
          math.max(candle.high, deal.price),
          math.min(candle.low, deal.price),
          deal.price,
          candle.volume + deal.size
        )
    }
  }

  private def cutTimestamp(timestamp: Timestamp) = {
    import java.util.Calendar
    val c = Calendar.getInstance
    c.setTime(timestamp)
    c.set(Calendar.SECOND, 0)
    Timestamp.from(c.toInstant)
  }
}
