package meetup

import zio.json._

import java.util.UUID

final case class Event(id: UUID, ownerId: UUID, name: String)

object Event {
  implicit val codec: JsonCodec[Event] =
    DeriveJsonCodec.gen
}
