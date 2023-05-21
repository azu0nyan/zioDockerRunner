package ru.azu.testRunner

sealed trait RunVerificationResult
object RunVerificationResult {
  final case class RunVerificationSuccess(message: Option[String]) extends RunVerificationResult
  final case class RunVerificationWrongAnswer(message: Option[String]) extends RunVerificationResult
}
/**
 * объект содержащий входные данные и необходимую информацию для верификации ответа
 */
trait SingleRun {
  val input: String
  def validate(out: String, timeMs: Long): RunVerificationResult
}


sealed trait CompileResult
sealed trait CompilationSuccess[LANGUAGE <: ProgrammingLanguage] extends CompileResult
sealed trait CompilationFailure extends CompileResult

object CompileResult {
  final case class CompilationError(errorMessage: String) extends CompilationFailure
  final case class CompilationServerError(errorMessage: Option[String]) extends CompilationFailure
  final case class RemoteWorkerConnectionError() extends CompilationFailure
  final case class RemoteWorkerCreationError() extends CompilationFailure
  final case class RemoteWorkerError(errorMessage: Option[String] = None) extends CompilationFailure

  final case class JavaCompilationSuccess(className: String) extends CompilationSuccess[ProgrammingLanguage.Java.type]
  final case class CppCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Cpp.type]
  final case class HaskellCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Haskell.type]
  final case class ScalaCompilationSuccess(path: String, classname: String) extends CompilationSuccess[ProgrammingLanguage.Scala.type]
}

sealed trait RunResult
object RunResult {
  final case class Success(output: String, timeMs: Long) extends RunResult
  final case class TimeLimitExceeded(timeMs: Long) extends RunResult
  final case class RuntimeError(errorMessage: String) extends RunResult
  final case class UnknownRunError(cause: String) extends RunResult
}

sealed trait ProgramRunResult
object ProgramRunResult {
  final case class ProgramRunResultSuccess(timeMS: Long, message: Option[String]) extends ProgramRunResult
  final case class ProgramRunResultWrongAnswer(message: Option[String]) extends ProgramRunResult
  final case class ProgramRunResultFailure(message: Option[String]) extends ProgramRunResult
  final case class ProgramRunResultTimeLimitExceeded(timeMs: Long) extends ProgramRunResult
  final case class ProgramRunResultNotTested() extends ProgramRunResult
}


enum ProgrammingLanguage:
  case  Java, Haskell, Scala,  Kojo, Cpp

type HardwareLimitations = Unit
type MultipleRunsResultScore = Seq[ProgramRunResult]




