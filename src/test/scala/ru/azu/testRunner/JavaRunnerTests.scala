package ru.azu.testRunner

import ru.azu.dockerIntegration.DockerOps
import ru.azu.dockerIntegration.DockerOps.{CopyArchiveToContainerParams, ExecuteCommandParams, ExecuteCommandResult}
import zio.test.ZIOSpecDefault
import ru.azu.testRunner.JavaRunner.ProgramSource
import ru.azu.testRunner.{CompilationError, JavaRunner}
import zio.ZLayer
import zio.test.*
import zio.test.Assertion.*

import java.io.ByteArrayInputStream
import scala.io.Source


object JavaRunnerTests extends ZIOSpecDefault{
  val testContainerName = "cont:0.1"

  def spec = suite("JavaRunnerTests")(
    compileProgram,
    compileIncorrectProgram,
    compileProgramWithNonDefaultMainClassName
  )

  val compileProgram = test("Compiling java program with Runner") {
    val javaFileText = Source.fromResource("java/WithMainClass.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .provideSomeLayer(DockerOps.dockerClientContextScoped(testContainerName))
      .exit

    assertZIO(toTest)(succeeds(equalTo(JavaCompilationSuccess("Main.java", "Main"))))
  }

  val compileIncorrectProgram = test("Compiling incorrect java program with Runner") {
    val javaFileText = Source.fromResource("java/IncorrectProgram.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .provideSomeLayer(DockerOps.dockerClientContextScoped(testContainerName))
      .exit

    assertZIO(toTest)(failsWithA[CompilationError])
  }

  val compileProgramWithNonDefaultMainClassName = test("Compiling java program with non default main class name Runner") {
    val javaFileText = Source.fromResource("java/WithNonDefaultMainClassName.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .provideSomeLayer(DockerOps.dockerClientContextScoped(testContainerName))
      .exit

    assertZIO(toTest)(succeeds(equalTo(JavaCompilationSuccess("NotMain.java", "NotMain"))))
  }



}
