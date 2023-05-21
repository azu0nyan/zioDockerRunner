package ru.azu.testRunner

import ru.azu.dockerIntegration.DockerOps
import ru.azu.dockerIntegration.DockerOps.{CopyArchiveToContainerParams, ExecuteCommandParams, ExecuteCommandResult}
import ru.azu.testRunner.CompileResult.{CompilationError, JavaCompilationSuccess}
import zio.test.ZIOSpecDefault
import ru.azu.testRunner.JavaRunner.ProgramSource
import ru.azu.testRunner.JavaRunner
import ru.azu.testRunner.RunResult.{RuntimeError, Success, TimeLimitExceeded}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import java.io.ByteArrayInputStream
import scala.io.Source


object JavaRunnerTests extends ZIOSpecDefault{
  val testContainerName = "cont:0.1"

  def spec = suite("JavaRunnerTests")(
    compileProgram,
    compileIncorrectProgram,
    compileProgramWithNonDefaultMainClassName,
    runProgram,
    runProgramRuntimeError,
    runTimeLimitExceeded,
    runLongRunning
  ).provideLayer(DockerOps.dockerClientContextScoped(testContainerName)) @@ timeout(10.seconds)

  val compileProgram = test("Compiling java program with Runner") {
    val javaFileText = Source.fromResource("java/WithMainClass.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .exit

    assertZIO(toTest)(succeeds(equalTo(JavaCompilationSuccess("Main"))))
  }

  val compileIncorrectProgram = test("Compiling incorrect java program with Runner") {
    val javaFileText = Source.fromResource("java/UncompillableProgram.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .exit

    assertZIO(toTest)(failsWithA[CompilationError])
  }

  val compileProgramWithNonDefaultMainClassName = test("Compiling java program with non default main class name Runner") {
    val javaFileText = Source.fromResource("java/WithNonDefaultMainClassName.java").mkString("")

    val toTest = JavaRunner
      .compileJava(ProgramSource(javaFileText))
      .exit

    assertZIO(toTest)(succeeds(equalTo(JavaCompilationSuccess("NotMain"))))
  }

  val runProgram = test("Running program and getting correct output"){
    val javaFileText = Source.fromResource("java/WithMainClass.java").mkString("")

    for{
      cs <- JavaRunner.compileJava(ProgramSource(javaFileText))
      exit <- JavaRunner.runCompiled(cs, "2", 10_000).exit
    } yield assert(exit)(succeeds(isSubtype[Success](hasField("output", _.output, equalTo("RESPONSE2\n")))))
  }

  val runProgramRuntimeError = test("Running program with runtime error ") {
    val javaFileText = Source.fromResource("java/WithRuntimeError.java").mkString("")

    for {
      cs <- JavaRunner.compileJava(ProgramSource(javaFileText))
      exit <- JavaRunner.runCompiled(cs, "", 10_000).exit
    } yield assert(exit)(succeeds(isSubtype[RuntimeError](Assertion.anything)))
  }

  val runTimeLimitExceeded = test("Running program with time limit exceeded error"){
    val javaFileText = Source.fromResource("java/WithTimeLimitExceeded.java").mkString("")

    for{
      cs <- JavaRunner.compileJava(ProgramSource(javaFileText))
      exit <- JavaRunner.runCompiled(cs, "", 500).exit
    } yield assert(exit)(succeeds(isSubtype[TimeLimitExceeded](Assertion.anything)))
  }

  val runLongRunning = test("Running long running program and get correct run time ") {
    val javaFileText = Source.fromResource("java/RunningMoreThan1000ms.java").mkString("")

    for {
      cs <- JavaRunner.compileJava(ProgramSource(javaFileText))
      exit <- JavaRunner.runCompiled(cs, "", 5000).exit
    } yield assert(exit)(succeeds(isSubtype[Success](hasField("timeMs", _.timeMs, isGreaterThan(1000L) && isLessThan(5000L)))))
  }



}
