package unformed.model

import java.sql.Timestamp

import io.circe.Encoder
import io.circe._, io.circe.generic.semiauto._

case class Candle
(
  ticker: String,
  timestamp: Timestamp,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Int
)

object Candle {
  implicit val timestampEncoder: Encoder[Timestamp] = new Encoder[Timestamp] {
    override def apply(a: Timestamp): Json = Encoder.encodeString.apply(a.toString)
  }

  implicit val candleEncoder: Encoder[Candle] = deriveEncoder[Candle]

}