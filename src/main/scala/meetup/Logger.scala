package meetup

import zio._
import zio.managed._
import zio.stream.ZStream

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Path

trait Logger {
  def log(msg: String): UIO[Unit]
}

final case class FileLoggerConfig(path: Path)

final case class FileLogger(queue: Queue[String]) extends Logger {
  override def log(msg: String): UIO[Unit] =
    queue.offer(msg).unit
}

object FileLogger {

  def make(config: FileLoggerConfig): ZManaged[Any, Throwable, FileLogger] =
    for {
      queue <- Queue.unbounded[String].toManaged
      fileWriter <-
        ZManaged.fromAutoCloseable(ZIO.attempt(new BufferedWriter(new FileWriter(config.path.toFile, true))))
      _ <- ZStream
             .fromQueue(queue)
             .foreach { s =>
               for {
                 now    <- Clock.instant
                 message = s"$now - $s\n"
                 _      <- ZIO.attempt { fileWriter.write(message); fileWriter.flush() }
               } yield ()
             }
             .forkManaged
    } yield FileLogger(queue)

  def layer(path: Path): ZLayer[Any, Throwable, FileLogger] =
    ZLayer.fromManaged {
      make(FileLoggerConfig(path))
    }
}
