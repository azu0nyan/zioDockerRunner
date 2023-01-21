ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "zioDockerRunner"
  )


libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.5",
  "dev.zio" %% "zio-streams" % "2.0.5",
//  "dev.zio" %% "zio-logging" % "2.1.3",
//  "dev.zio" %% "zio-process" % "0.7.1",
  "org.apache.commons" % "commons-compress" % "1.22",
  "com.github.docker-java" % "docker-java" % "3.2.14",
)

//Testing
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.0.5" % Test,
  "dev.zio" %% "zio-test-sbt"      % "2.0.5" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.0.5" % Test
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")