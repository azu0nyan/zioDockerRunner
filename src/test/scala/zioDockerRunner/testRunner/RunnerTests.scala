package zioDockerRunner.testRunner


import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zioDockerRunner.dockerIntegration.DockerOps
import zioDockerRunner.testRunner.CompileResult.CompilationError
import zioDockerRunner.testRunner.JavaRunnerTests.testContainerName
import zioDockerRunner.testRunner.RunResult.{CorrectAnswer, NotTested, SuccessffulRun, WrongAnswer}
import zioDockerRunner.testRunner.RunVerificationResult.{RunVerificationSuccess, RunVerificationWrongAnswer}

import scala.io.Source

object RunnerTests extends ZIOSpecDefault {
  def spec = suite("Runner tests")(
    compileAndRun,
    uncompillable
  ).provideLayer(DockerOps.dockerClientContextScoped(testContainerName)) @@ timeout(10.seconds) @@ withLiveClock

  val compileAndRun = test("Running multiple run") {
    val javaFileText = Source.fromResource("java/EchoLine.java").mkString("")
    val crm = CompileAndRunMultiple(
      ProgramSource(javaFileText),
      ProgrammingLanguage.Java,
      Seq(new SingleRun { //CorrectAnswer
        override val input: String = "A"
        override def validate(out: String): RunVerificationResult =
          if (out == "A") RunVerificationSuccess(None) else RunVerificationWrongAnswer(None)
      }, new SingleRun { //CorrectAnswer
        override val input: String = "B"
        override def validate(out: String): RunVerificationResult =
          if (out == "B") RunVerificationSuccess(None) else RunVerificationWrongAnswer(None)
      }, new SingleRun { //WRONG ANSWER
        override val input: String = "Q"
        override def validate(out: String): RunVerificationResult =
          if (out == "A") RunVerificationSuccess(None) else RunVerificationWrongAnswer(None)
      }, new SingleRun { //NOT TESTED
        override val input: String = "Q"
        override def validate(out: String): RunVerificationResult =
          if (out == "A") RunVerificationSuccess(None) else RunVerificationWrongAnswer(None)
      }),
      HardwareLimitations()
    )

    for {
      res <- Runner.compileAndRunMultiple(crm).exit
    } yield assert(res)(succeeds(isSubtype[MultipleRunsResultScore](
      hasField[MultipleRunsResultScore, Int]("size", _.size, equalTo(4)) &&
        hasField("el0", s => s(0), isSubtype[CorrectAnswer](anything)) &&
        hasField("el1", s => s(1), isSubtype[CorrectAnswer](anything)) &&
        hasField("el2", s => s(2), isSubtype[WrongAnswer](anything)) &&
        hasField("el3", s => s(3), isSubtype[NotTested](anything))
    )))
  }

  val uncompillable = test("uncompillable") {
    val javaFileText = Source.fromResource("java/UncompillableProgram.java").mkString("")
    println(javaFileText)
    val crm = CompileAndRunMultiple(
      ProgramSource(javaFileText),
      ProgrammingLanguage.Java,
      Seq(),
      HardwareLimitations()
    )

    for {
      res <- Runner.compileAndRunMultiple(crm).exit
    } yield assert(res)(succeeds(isSubtype[CompilationError](Assertion.anything)))
  }


}
