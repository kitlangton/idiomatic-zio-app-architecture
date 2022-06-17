package meetup

import zio.{Random, Ref, Task, UIO, ZEnvironment, ZIO}

import java.util.UUID
import javax.sql.DataSource
import QuillContext._
import zio.managed.ZManaged

trait Users {
  def get(id: UUID): Task[Option[User]]
  def all: Task[List[User]]
  def create(email: String): Task[User]
}

final case class UsersLive(
  dataSource: DataSource,
  analytics: Analytics,
  logger: Logger
) extends Users {
  val quillEnv: ZEnvironment[DataSource] = ZEnvironment(dataSource)

  override def get(id: UUID): Task[Option[User]] =
    run(query[User].filter(_.id == lift(id)))
      .map(_.headOption)
      .provideEnvironment(quillEnv)

  override def all: Task[List[User]] =
    run(query[User])
      .provideEnvironment(quillEnv)

  override def create(email: String): Task[User] =
    for {
      id  <- Random.nextUUID
      user = User(id, email)
      _ <- run(query[User].insertValue(lift(user)))
             .provideEnvironment(quillEnv)
    } yield user

}

final case class FakeUsersService(ref: Ref[Map[UUID, User]]) extends Users {
  override def get(id: UUID): Task[Option[User]] =
    ref.get.map(_.get(id))

  override def all: Task[List[User]] =
    ref.get.map(_.values.toList)

  override def create(email: String): Task[User] =
    for {
      id  <- Random.nextUUID
      user = User(id, email)
      _   <- ref.update(_ + (id -> user))
    } yield user
}

object FakeUsersService {
  val make: UIO[Users] =
    for {
      ref <- Ref.make(Map.empty[UUID, User])
    } yield FakeUsersService(ref)
}

object UsersLive {
  def managed(dataSource: DataSource, analytics: Analytics, logger: Logger): ZManaged[Any, Nothing, Users] =
    Utils.makeManaged("EventsLive", UsersLive(dataSource, analytics, logger))
}
