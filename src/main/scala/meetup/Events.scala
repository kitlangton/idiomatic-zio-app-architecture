package meetup

import zio._
import QuillContext._
import zio.managed.ZManaged

import java.util.UUID
import javax.sql.DataSource

trait Events {
  def get(id: UUID): Task[Option[Event]]
  def all: Task[List[Event]]
  def create(ownerId: UUID, name: String): Task[Event]
}

final case class EventsLive(
  dataSource: DataSource,
  analytics: Analytics,
  logger: Logger
) extends Events {

  val quillEnv: ZEnvironment[DataSource] = ZEnvironment(dataSource)

  override def get(id: UUID): Task[Option[Event]] =
    for {
      _ <- logger.log(s"Fetching event $id")
      event <-
        run(query[Event].filter(_.id == lift(id)))
          .map(_.headOption)
          .provideEnvironment(quillEnv)
    } yield event

  override def all: Task[List[Event]] =
    for {
      _ <- logger.log("Fetching all events")
      events <- run(query[Event])
                  .provideEnvironment(quillEnv)
    } yield events

  override def create(ownerId: UUID, name: String): Task[Event] =
    for {
      _    <- logger.log(s"Creating event $name with owner $ownerId")
      id   <- Random.nextUUID
      event = Event(id, ownerId, name)
      _ <- run(query[Event].insertValue(lift(event)))
             .provideEnvironment(quillEnv)
      _ <- analytics.emit("EventCreated", id.toString)
    } yield event

}

object EventsLive {
  def managed(dataSource: DataSource, analytics: Analytics, logger: Logger): ZManaged[Any, Nothing, Events] =
    Utils.makeManaged("EventsLive", EventsLive(dataSource, analytics, logger))
}
