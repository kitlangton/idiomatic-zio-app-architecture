package meetup

import QuillContext._
import zio.managed.ZManaged
import zio.{Task, ZEnvironment, ZLayer}

import java.util.UUID
import javax.sql.DataSource

trait Rsvps {
  def allForEvent(eventId: UUID): Task[List[Rsvp]]
  def create(eventId: UUID, userId: UUID): Task[Rsvp]
  def delete(eventId: UUID, userId: UUID): Task[Unit]

}

final case class RsvpsLive(
  notifications: Notifications,
  dataSource: DataSource,
  analytics: Analytics,
  logger: Logger
) extends Rsvps {
  private val quillEnv = ZEnvironment(dataSource)

  override def allForEvent(eventId: UUID): Task[List[Rsvp]] =
    for {
      _ <- logger.log(s"Fetching all RSVPs for event $eventId")
      rsvps <- run(query[Rsvp].filter(_.eventId == lift(eventId)))
                 .provideEnvironment(quillEnv)
    } yield rsvps

  override def create(eventId: UUID, userId: UUID): Task[Rsvp] =
    for {
      _   <- logger.log(s"Creating RSVP for event $eventId with user $userId")
      rsvp = Rsvp(eventId, userId)
      _   <- run(query[Rsvp].insertValue(lift(rsvp))).provideEnvironment(quillEnv)
      _   <- notifications.notifyOwner(rsvp)
    } yield rsvp

  override def delete(eventId: UUID, userId: UUID): Task[Unit] =
    for {
      _ <- logger.log(s"Deleting RSVP for event $eventId with user $userId")
      _ <- run(
             query[Rsvp]
               .filter(rsvp => rsvp.eventId == lift(eventId) && rsvp.userId == lift(userId))
               .delete
           )
             .provideEnvironment(quillEnv)
    } yield ()
}

object RsvpsLive {
  val layer =
    ZLayer.fromFunction(RsvpsLive.apply _)

  def managed(
    analytics: Analytics,
    notifications: Notifications,
    dataSource: DataSource,
    logger: Logger
  ): ZManaged[Any, Nothing, RsvpsLive] =
    Utils.makeManaged("RsvpsLive", RsvpsLive(notifications, dataSource, analytics, logger))

}

trait Notifications {
  def notifyOwner(rsvp: Rsvp): Task[Unit]
}

final case class NotificationsLive(
  events: Events,
  users: Users,
  analytics: Analytics,
  emailService: EmailService
) extends Notifications {
  override def notifyOwner(rsvp: Rsvp): Task[Unit] =
    for {
      _        <- analytics.emit("Cool", "Beans")
      event    <- events.get(rsvp.eventId).someOrFailException
      attendee <- users.get(rsvp.userId).someOrFailException
      owner    <- users.get(event.ownerId).someOrFailException
      _ <- emailService.send(
             s"${attendee.email} has RSVP'd for ${event.name}",
             owner.email
           )
    } yield ()
}

object NotificationsLive {
  val layer =
    ZLayer.fromFunction(NotificationsLive.apply _)

  def managed(
    events: Events,
    users: Users,
    analytics: Analytics,
    emailService: EmailService
  ): ZManaged[Any, Nothing, NotificationsLive] =
    Utils.makeManaged("NotificationsLive", NotificationsLive(events, users, analytics, emailService))
}
