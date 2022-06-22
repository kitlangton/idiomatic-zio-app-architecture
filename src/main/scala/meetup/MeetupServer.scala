package meetup

import zhttp.http._
import zhttp.service.Server
import zio.{ZIO, ZLayer}
import zio.json._

import java.util.UUID

// DataSource & Analytics & Logger => UsersLive
// Users & Events & Rsvps & Logger => MeetupServer
final case class MeetupServer(
  users: Users,
  events: Events,
  rsvps: Rsvps,
  logger: Logger
) {
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

      // Rsvp Routes

      case Method.GET -> !! / "rsvps" / eventId =>
        for {
          rsvps <- rsvps.allForEvent(UUID.fromString(eventId))
        } yield Response.json(rsvps.toJson)

      case req @ Method.POST -> !! / "rsvps" =>
        for {
          body       <- req.bodyAsString
          createRsvp <- ZIO.from(body.fromJson[CreateRsvp].left.map(new Error(_)))
          rsvp       <- rsvps.create(eventId = createRsvp.eventId, userId = createRsvp.userId)
        } yield Response.json(rsvp.toJson)

    }

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, routes)
}

object MeetupServer {
  val layer: ZLayer[Users with Events with Rsvps with Logger, Nothing, MeetupServer] =
    ZLayer.fromFunction(MeetupServer.apply _)
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

final case class CreateRsvp(userId: UUID, eventId: UUID)

object CreateRsvp {
  implicit val codec: JsonCodec[CreateRsvp] = DeriveJsonCodec.gen
}
