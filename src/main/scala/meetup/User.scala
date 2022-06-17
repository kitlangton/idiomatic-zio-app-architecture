package meetup

import zio.json._

import java.util.UUID

final case class User(id: UUID, email: String)

object User {
  implicit val codec: JsonCodec[User] =
    DeriveJsonCodec.gen
}
