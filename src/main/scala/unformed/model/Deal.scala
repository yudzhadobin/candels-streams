package unformed.model

import java.sql.Timestamp

import scodec.Codec
import scodec.codecs._

case class Deal
(
    len: Int,
    timestamp: Timestamp,
    tickerLength: Int,
    ticker: String,
    price: Double,
    size: Int
)

object Deal {
  val codec: Codec[Deal] = {
    (("len" | uint16) :: ("timestamp" | int64.xmap[Timestamp](long => new Timestamp(long), timestamp => timestamp.getTime))) :::
      (("tickerLength" | int16)  >>:~ { length =>
        ("ticker" | bits(length * 8).exmap[String](bitVector => ascii.decodeValue(bitVector), string => ascii.encode(string))).hlist
      }) ::: (("price" | double) :: ("size" | int32))
  }.as[Deal]

}