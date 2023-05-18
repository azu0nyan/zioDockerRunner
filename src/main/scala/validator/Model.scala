package validator


sealed trait RunVerificationResult
case class RunVerificationSuccess(message: Option[String]) extends RunVerificationResult
case class RunVerificationWrongAnswer(message: Option[String]) extends RunVerificationResult

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

case class CompilationError(errorMessage: String) extends CompilationFailure
case class CompilationServerError(errorMessage: Option[String]) extends CompilationFailure
case class RemoteWorkerConnectionError() extends CompilationFailure
case class RemoteWorkerCreationError() extends CompilationFailure
case class RemoteWorkerError(errorMessage: Option[String] = None) extends CompilationFailure

case class JavaCompilationSuccess(path: String, classname: String) extends CompilationSuccess[ProgrammingLanguage.Java.type]
case class CppCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Cpp.type]
case class HaskellCompilationSuccess(path: String, filename: String) extends CompilationSuccess[ProgrammingLanguage.Haskell.type]
case class ScalaCompilationSuccess(path: String, classname: String) extends CompilationSuccess[ProgrammingLanguage.Scala.type]


sealed trait RunResult
case class Success(output: String, timeMs: Long) extends RunResult
case class TimeLimitExceeded(timeMs: Long) extends RunResult
case class RuntimeError(errorMessage: String) extends RunResult
case class UnknownRunError(cause: String) extends RunResult


sealed trait ProgramRunResult
case class ProgramRunResultSuccess(timeMS: Long, message: Option[String]) extends ProgramRunResult
case class ProgramRunResultWrongAnswer(message: Option[String]) extends ProgramRunResult
case class ProgramRunResultFailure(message: Option[String]) extends ProgramRunResult
case class ProgramRunResultTimeLimitExceeded(timeMs: Long) extends ProgramRunResult
case class ProgramRunResultNotTested() extends ProgramRunResult


enum ProgrammingLanguage:
  case  Java, Haskell, Scala,  Kojo, Cpp

type HardwareLimitations = Unit
type MultipleRunsResultScore = Seq[ProgramRunResult]




