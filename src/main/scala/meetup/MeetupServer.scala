package meetup

import zhttp.http._
import zhttp.service.Server
import zio.ZIO
import zio.json._

import java.util.UUID

final case class MeetupServer(users: Users, events: Events, logger: Logger) {
  private val routes: HttpApp[Any, Throwable] =
    Http.collectZIO[Request] {

      // User Routes

      case Method.GET -> !! / "users" / id =>
        users.get(UUID.fromString(id)).map { user =>
          Response.json(user.toJson)
        }

      case Method.GET -> !! / "users" =>
        users.all.map { users =>
          Response.json(users.toJson)
        }

      case req @ Method.POST -> !! / "users" =>
        for {
          body       <- req.bodyAsString
          createUser <- ZIO.from(body.fromJson[CreateUser].left.map(new Error(_)))
          user       <- users.create(createUser.email)
        } yield Response.json(user.toJson)

      // Event Routes

      case Method.GET -> !! / "events" / id =>
        events.get(UUID.fromString(id)).map { event =>
          Response.json(event.toJson)
        }

      case Method.GET -> !! / "events" =>
        events.all.map { events =>
          Response.json(events.toJson)
        }

      case req @ Method.POST -> !! / "events" =>
        for {
          body        <- req.bodyAsString
          createEvent <- ZIO.from(body.fromJson[CreateEvent].left.map(new Error(_)))
          event       <- events.create(createEvent.ownerId, createEvent.name)
        } yield Response.json(event.toJson)
    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, routes)
}

// API Models

final case class CreateUser(email: String)

object CreateUser {
  implicit val codec: JsonCodec[CreateUser] = DeriveJsonCodec.gen
}

final case class CreateEvent(ownerId: UUID, name: String)

object CreateEvent {
  implicit val codec: JsonCodec[CreateEvent] = DeriveJsonCodec.gen
}
