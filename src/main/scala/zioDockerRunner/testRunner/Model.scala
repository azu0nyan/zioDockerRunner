package zioDockerRunner.testRunner

import zioDockerRunner.testRunner

type CompileAndRunMultipleResult = CompilationFailure | MultipleRunsResultScore

case class ProgramSource(src: String)
case class CompileAndRunMultiple(
                                  programCode: ProgramSource,
                                  language: ProgrammingLanguage,
                                  runs: Seq[SingleRun],
                                  limitations: HardwareLimitations)



/**
 * объект содержащий входные данные и необходимую информацию для верификации ответа
 */
trait SingleRun {
  val input: String
  def validate(out: String): RunVerificationResult
}


sealed trait CompileResult
sealed trait CompilationSuccess[LANGUAGE <: ProgrammingLanguage] extends CompileResult
sealed trait CompilationFailure extends CompileResult

object CompileResult {
  final case class CompilationError(errorMessage: String) extends CompilationFailure
//  final case class CompilationServerError(errorMessage: Option[String]) extends CompilationFailure
//  final case class RemoteWorkerConnectionError() extends CompilationFailure
//  final case class RemoteWorkerCreationError() extends CompilationFailure
  final case class RemoteWorkerError(errorMessage: Option[String] = None) extends CompilationFailure

  final case class JavaCompilationSuccess(className: String) extends CompilationSuccess[ProgrammingLanguage.Java.type]
  final case class CppCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Cpp.type]
  final case class HaskellCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Haskell.type]
  final case class ScalaCompilationSuccess(path: String, classname: String) extends CompilationSuccess[ProgrammingLanguage.Scala.type]
}

/**Результат запуска и верефикации, видимый пользователю*/
sealed trait UserRunResult

/**Результат запуска в докере*/
sealed trait RawRunResult



object RunResult {
  final case class Success(output: String, timeMs: Long) extends RawRunResult
  
  final case class RuntimeError(errorMessage: String) extends RawRunResult with UserRunResult
  final case class UnknownRunError(cause: String) extends RawRunResult with UserRunResult
  final case class TimeLimitExceeded(timeMs: Long) extends RawRunResult with UserRunResult
  final case class MemoryLimitExceeded(memory: Memory) extends RawRunResult with UserRunResult

  final case class CorrectAnswer(timeMS: Long, message: Option[String]) extends UserRunResult
  final case class WrongAnswer(message: Option[String]) extends UserRunResult  
  final case class NotTested(message: Option[String]) extends UserRunResult  
}

/**Результат проверки ответа */
sealed trait RunVerificationResult
object RunVerificationResult {
  final case class RunVerificationSuccess(message: Option[String]) extends RunVerificationResult
  final case class RunVerificationWrongAnswer(message: Option[String]) extends RunVerificationResult
}




enum ProgrammingLanguage:
  case  Java, Haskell, Scala,  Kojo, Cpp

type Memory = Long
case class HardwareLimitations(memoryLimit: Memory = 128, timeLimitSeconds: Double = 2, cpuLimit: Double = 1)
type MultipleRunsResultScore = Seq[UserRunResult]




