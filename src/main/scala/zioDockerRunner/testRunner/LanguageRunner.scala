package zioDockerRunner.testRunner

import zioDockerRunner.dockerIntegration.DockerOps.DockerClientContext
import zio.*
import zioDockerRunner.testRunner.RunResult.{CorrectAnswer, NotTested, WrongAnswer}
import RunVerificationResult.*

trait LanguageRunner[L <: ProgrammingLanguage] {
  type CompilationSuccessL <: CompilationSuccess[L]

  def compile(source: ProgramSource): ZIO[DockerClientContext, CompilationFailure, CompilationSuccessL]

  def runCompiled(compilationSuccess: CompilationSuccessL, input: String, maxTime: Long): ZIO[DockerClientContext, Nothing, RawRunResult]

  def runSingleRun(compilationSuccess: CompilationSuccessL, maxTime: Long, s: SingleRun): ZIO[DockerClientContext, Nothing, UserRunResult] = {
    for {
      res <- runCompiled(compilationSuccess, s.input, maxTime)
    } yield res match
      case urr: UserRunResult => urr
      case RunResult.Success(output, timeMs) =>
        s.validate(output) match
          case RunVerificationSuccess(message) => CorrectAnswer(timeMs, message)
          case RunVerificationWrongAnswer(message) => WrongAnswer(message)
  }

  def runTestsInCompiled(crm: CompileAndRunMultiple, succ: CompilationSuccessL): ZIO[DockerClientContext, Nothing, MultipleRunsResultScore] = {
    case class Acc(continue: Boolean = false, acc: Seq[UserRunResult] = Seq())

    ZIO.foldLeft(crm.runs)(Acc()) {
      case (Acc(true, acc), run) =>
        for (userRunRes <- runSingleRun(succ, (crm.limitations.timeLimitSeconds * 1000).toLong, run))
          yield userRunRes match
            case ca: CorrectAnswer => Acc(true, acc :+ ca)
            case other => Acc(false, acc :+ other)
      case (Acc(false, acc), run) => ZIO.succeed(Acc(false, acc :+ NotTested(None)))
    }.map(_.acc)
  }

  def compileAndRunMultiple(crm: CompileAndRunMultiple): ZIO[DockerClientContext, Nothing, CompileAndRunMultipleResult] = {
    val success = for {
      compSuccess <- compile(crm.programCode)
      res <- runTestsInCompiled(crm, compSuccess)
    } yield res

    success.catchAll {
      case cf: CompilationFailure => ZIO.succeed(cf)
    }
  }

}
