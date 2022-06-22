package meetup

import zio._
import java.nio.file.Path

object Main extends ZIOAppDefault {

  override val run =
    ZIO
      .serviceWithZIO[MeetupServer](_.start)
      .provide(
        MeetupServer.layer,
        UsersLive.layer,
        RsvpsLive.layer,
        EventsLive.layer,
        QuillContext.dataSourceLayer,
        AnalyticsLive.layer,
        NotificationsLive.layer,
        EmailServiceLive.layer,
        fileLogger
      )

  private lazy val fileLogger =
    FileLogger.layer(Path.of("/Users/kit/code/zlayer-example/src/main/resources/log.txt"))

  private val manualProgram = {
    val config  = FileLoggerConfig(Path.of("/Users/kit/code/zlayer-example/src/main/resources/log.txt"))
    val managed = FileLogger.make(config) zip QuillContext.dataSourceManaged
    for {
      _ <- managed.use { case (logger, dataSource) =>
             AnalyticsLive.managed(logger).use { analytics =>
               UsersLive.managed(dataSource, analytics, logger).use { users =>
                 EventsLive.managed(dataSource, analytics, logger).use { events =>
                   val emailService = EmailServiceLive(logger)
                   NotificationsLive.managed(events, users, analytics, emailService).use { notifications =>
                     RsvpsLive.managed(analytics, notifications, dataSource, logger).use { rsvps =>
                       val server = MeetupServer(users, events, rsvps, logger)
                       server.start
                     }
                   }
                 }
               }
             }
           }
    } yield ()
  }
}
