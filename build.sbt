ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val zioVersion = "2.0.0-RC6"

lazy val root = (project in file("."))
  .settings(
    name := "zlayer-example",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"            % zioVersion,
      "dev.zio"       %% "zio-managed"    % zioVersion,
      "dev.zio"       %% "zio-macros"     % zioVersion,
      "dev.zio"       %% "zio-streams"    % zioVersion,
      "dev.zio"       %% "zio-test"       % zioVersion % Test,
      "dev.zio"       %% "zio-test-sbt"   % zioVersion % Test,
      "dev.zio"       %% "zio-json"       % "0.3.0-RC8",
      "io.d11"        %% "zhttp"          % "2.0.0-RC9",
      "io.getquill"   %% "quill-jdbc-zio" % "4.0.0-RC1",
      "org.postgresql" % "postgresql"     % "42.3.6"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
