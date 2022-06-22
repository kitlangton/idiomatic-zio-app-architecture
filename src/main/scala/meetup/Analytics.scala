package meetup

import zio._
import zio.managed._

trait Analytics {
  def emit(name: String, value: String): UIO[Unit]
}

final case class AnalyticsLive(logger: Logger) extends Analytics {
  override def emit(name: String, value: String): UIO[Unit] =
    logger.log(s"Emitting Analytics: $name: $value")
}

object AnalyticsLive {
  val layer: ZLayer[Logger, Nothing, Analytics] =
    ZLayer.fromFunction(AnalyticsLive.apply _)

  def managed(logger: Logger): ZManaged[Any, Nothing, AnalyticsLive] =
    Utils.makeManaged("AnalyticsLive", AnalyticsLive(logger))
}
