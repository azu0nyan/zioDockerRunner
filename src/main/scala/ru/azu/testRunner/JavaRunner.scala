package ru.azu.testRunner

import ru.azu.dockerIntegration.{CompressOps, DockerOps}
import ru.azu.dockerIntegration.DockerOps.{CopyArchiveToContainerParams, DockerClientContext, ExecuteCommandParams}
import ru.azu.testRunner.CompileResult.{CompilationError, JavaCompilationSuccess, RemoteWorkerError}
import ru.azu.testRunner.RunResult.{RuntimeError, Success, UnknownRunError}
import zio.*

import java.io.ByteArrayInputStream

object JavaRunner {
  case class ProgramSource(src: String)

  case class CompileAndRunMultiple(
                                    programCode: ProgramSource,
                                    language: ProgrammingLanguage,
                                    runs: Seq[SingleRun],
                                    limitations: HardwareLimitations)

  def extractJavaMainClassName(src: String): Option[String] = {
    val regex = "public[\\s]+(final[\\s]+)?class[\\s]+([a-zA-Z_$][0-9a-zA-Z_$]*)".r
    regex.findFirstMatchIn(src).flatMap { m =>
      if (m.groupCount >= 2) Some(m.group(2))
      else None
    }
  }

  def compileJava(source: ProgramSource): ZIO[DockerClientContext, CompilationFailure, JavaCompilationSuccess] =
    for {
      className <- ZIO.fromOption(extractJavaMainClassName(source.src)).mapError(_ => CompilationError("No public class with main found"))
      tarStream <- ZIO.succeed(CompressOps.asTarStream(source.src, s"$className.java"))
      _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream)).mapError(_ => RemoteWorkerError(Some("Cant copy archive to container")))
      compileRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(Seq("javac", s"$className.java"), None)).mapError(_ => RemoteWorkerError(Some("Error while compiling with javac ")))
      r <-
        if (compileRes.exitCode.contains(1)) ZIO.fail(CompilationError(compileRes.stdOut))
        else ZIO.succeed(JavaCompilationSuccess(className))
    } yield r

  def runCompiled(compilationSuccess: JavaCompilationSuccess, input: String, maxTime: Long): ZIO[DockerClientContext, Nothing, RunResult] = {
    val inputStream = new ByteArrayInputStream("2".getBytes("UTF-8"))
    val runCommand = Seq("java", compilationSuccess.className)
    (for {
      runRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(runCommand, Some(inputStream)))
    } yield if (runRes.exitCode.contains(1)) RuntimeError(runRes.stdOut)
            else Success(runRes.stdOut, 0)).catchAll(e => ZIO.succeed(UnknownRunError(e.toString)))

  }



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
