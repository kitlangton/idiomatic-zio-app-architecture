package meetup

import zio._
import zio.managed._

object Utils {
  def makeManaged[Service](name: String, service: Service): ZManaged[Any, Nothing, Service] =
    for {
      _ <- ZIO.debug(s"Starting $name").toManaged
      _ <- ZManaged.finalizer(ZIO.debug(s"Shutting down $name"))
    } yield service
}
