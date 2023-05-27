package zioDockerRunner.testRunner

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zioDockerRunner.testRunner.ConcurrentRunner.ConcurrentRunnerConfig
import zioDockerRunner.testRunner.RunResult.{CorrectAnswer, NotTested, WrongAnswer}
import zioDockerRunner.testRunner.RunVerificationResult.{RunVerificationSuccess, RunVerificationWrongAnswer}

import scala.io.Source

object ConcurrentRunnerTests extends ZIOSpecDefault{
  val testContainerName = "cont:0.1"

  val javaFileText = Source.fromResource("java/EchoLineDelay.java").mkString("")
  val nonTrivialCrM = CompileAndRunMultiple(
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

  def spec = suite("JavaRunnerTests")(
  //  basicCompileRun,
    runMultipleParallel
  )

  val basicCompileRun = test("Running single") {

    val conf = ConcurrentRunnerConfig(fibersMax = 6, containerName = testContainerName)
    val crm = nonTrivialCrM

    for {
      concurrentRunner <- ConcurrentRunner.makeRunner(Seq(conf), 16)
      _ <- concurrentRunner.startWorkers
//      _ <- ZIO.foreachDiscard(0 to 2)(_ => concurrentRunner.addTask(crm))
      prom <- concurrentRunner.addTask(crm)
      res <- prom.await.exit
    } yield assert(res)(succeeds(isSubtype[MultipleRunsResultScore](
      hasField[MultipleRunsResultScore, Int]("size", _.size, equalTo(4)) &&
        hasField("el0", s => s(0), isSubtype[CorrectAnswer](anything)) &&
        hasField("el1", s => s(1), isSubtype[CorrectAnswer](anything)) &&
        hasField("el2", s => s(2), isSubtype[WrongAnswer](anything)) &&
        hasField("el3", s => s(3), isSubtype[NotTested](anything))
    )))
  }@@ timeout(10.seconds) @@ withLiveClock

  //todo better test
  val runMultipleParallel = test("Running multiple parallel") {

    val conf = ConcurrentRunnerConfig(fibersMax = 4, containerName = testContainerName)
    val crm = nonTrivialCrM

    for {
      concurrentRunner <- ConcurrentRunner.makeRunner(Seq(conf), 16)
      _ <- concurrentRunner.startWorkers
       _ <- ZIO.foreach(0 to 16)(_ => concurrentRunner.addTask(crm))
      prom <- concurrentRunner.addTask(crm)
      res <- prom.await.exit
    } yield assert(res)(succeeds(isSubtype[MultipleRunsResultScore](
      hasField[MultipleRunsResultScore, Int]("size", _.size, equalTo(4)) &&
        hasField("el0", s => s(0), isSubtype[CorrectAnswer](anything)) &&
        hasField("el1", s => s(1), isSubtype[CorrectAnswer](anything)) &&
        hasField("el2", s => s(2), isSubtype[WrongAnswer](anything)) &&
        hasField("el3", s => s(3), isSubtype[NotTested](anything))
    )))
  }@@ timeout(30.seconds) @@ withLiveClock
}
