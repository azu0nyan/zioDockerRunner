ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "zioDockerRunner"
  )


val zioVersion = "2.0.13"
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  // ZIO Logging backends
  "dev.zio" %% "zio-logging" % "2.1.13",
//  "dev.zio" %% "zio-process" % "0.7.1",
  "org.apache.commons" % "commons-compress" % "1.22",
  "com.github.docker-java" % "docker-java" % "3.2.14",
)

//libraryDependencies ++= Seq(
//  "dev.zio" %% "zio-core"    % "2.0.5" % Test,
//  "dev.zio" %% "zio-streams" % "2.0.5" % Test
//)

//Testing
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt"      % zioVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")