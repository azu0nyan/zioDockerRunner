package validator

import runner.{CompressOps, DockerOps}
import runner.DockerOps.{CopyArchiveToContainerParams, DockerClientContext, ExecuteCommandParams}
import zio.*

object Runner {
  case class ProgramSource(src: String)

  case class CompileAndRunMultiple(
                                    programCode: ProgramSource,
                                    language: ProgrammingLanguage,
                                    runs: Seq[SingleRun],
                                    limitations: HardwareLimitations)

  def compileJava(source: ProgramSource): ZIO[DockerClientContext, CompilationFailure, CompilationSuccess[ProgrammingLanguage.Java.type]] =
    for {
      context <- ZIO.service[DockerClientContext]
      tarStream <- ZIO.succeed(CompressOps.asTarStream(source.src, "Main.java"))
      _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream)).mapError(_ => RemoteWorkerError(Some("Cant copy archive to container")))
      res <- DockerOps.executeCommandInContainer(ExecuteCommandParams(Seq("javac", "Main.java"), None)).mapError(_ => RemoteWorkerError(Some("Cant copy archive to container")))
      _ <- if(!res.exitCode.contains(1)) ZIO.fail(CompilationError(res.stdOut)) else ZIO.succeed(JavaCompilationSuccess("", ""))
    } yield JavaCompilationSuccess("", "")//ZIO.fail(CompilationError(""))



  /*
    case class CompileAndRunMultiple(
                                      programCode: ProgramSource,
                                      language: ProgrammingLanguage,
                                      runs: Seq[SingleRun],
                                      limitations: HardwareLimitations)



  */

//  type CompileAndRunMultipleResult = CompilationFailure | MultipleRunsResultScore
//
//  def compile[L <: ProgrammingLanguage: Tag]: ZIO[DockerClientContext, CompilationFailure, CompilationSuccess[L]] = ???
//
//  def compileAndRunMultiple: ZIO[DockerClientContext & CompileAndRunMultiple, Throwable, CompileAndRunMultipleResult] = ???

//    for {
//      dc <- ZIO.service[DockerClient]
//      c <- ZIO.service[Container]
//      res <- ZIO.attempt(executeCommandInContainer(dc, c, params))
//    } yield res


}
