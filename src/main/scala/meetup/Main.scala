package meetup

import zio._
import java.nio.file.Path

// - MeetupServer
//   - Users
//     - Database
//     - Logger
//     - Analytics
//   - Events
//     - Database
//     - Logger
//     - Analytics
object Main extends ZIOAppDefault {

  val program = {
    val config  = FileLoggerConfig(Path.of("/Users/kit/code/zlayer-example/src/main/resources/log.txt"))
    val managed = FileLogger.make(config) zip QuillContext.dataSourceManaged
    for {
      _ <- managed.use { case (logger, dataSource) =>
             AnalyticsLive.managed(logger).use { analytics =>
               val users  = UsersLive(dataSource, analytics, logger)
               val events = EventsLive(dataSource, analytics, logger)
               val server = MeetupServer(users, events, logger)
               server.start
             }
           }
    } yield ()
  }

  val run =
    program
}
