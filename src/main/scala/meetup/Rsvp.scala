package meetup

import zio.json._

import java.util.UUID

final case class Rsvp(eventId: UUID, userId: UUID)

object Rsvp {
  implicit val codec: JsonCodec[Rsvp] =
    DeriveJsonCodec.gen
}
