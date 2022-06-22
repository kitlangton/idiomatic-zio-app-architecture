package meetup

import zio.{Task, ZLayer}

trait EmailService {
  def send(message: String, to: String): Task[Unit]
}

final case class EmailServiceLive(logger: Logger) extends EmailService {
  override def send(message: String, to: String): Task[Unit] =
    logger.log(s"Sending email to $to: $message")
}

object EmailServiceLive {
  val layer: ZLayer[Logger, Nothing, EmailService] =
    ZLayer.fromFunction(EmailServiceLive.apply _)
}
