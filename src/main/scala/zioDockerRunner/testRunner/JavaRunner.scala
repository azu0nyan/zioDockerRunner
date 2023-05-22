package zioDockerRunner.testRunner

import zioDockerRunner.dockerIntegration.DockerOps.{CopyArchiveToContainerParams, DockerClientContext, ExecuteCommandParams}
import CompileResult.{CompilationError, JavaCompilationSuccess, RemoteWorkerError}
import RunResult.{RuntimeError, Success, TimeLimitExceeded, UnknownRunError}
import zio.*
import zio.Console.printLine
import zioDockerRunner.dockerIntegration.{CompressOps, DockerOps}

import java.util.concurrent.TimeUnit
import java.io.ByteArrayInputStream

given JavaRunner: LanguageRunner [ProgrammingLanguage.Java.type] with {
  override type CompilationSuccessL = JavaCompilationSuccess

  def extractJavaMainClassName(src: String): Option[String] = {
    val regex = "public[\\s]+(final[\\s]+)?class[\\s]+([a-zA-Z_$][0-9a-zA-Z_$]*)".r
    regex.findFirstMatchIn(src).flatMap { m =>
      if (m.groupCount >= 2) Some(m.group(2))
      else None
    }
  }

  def compile(source: ProgramSource): ZIO[DockerClientContext, CompilationFailure, JavaCompilationSuccess] =
    for {
      className <- ZIO.fromOption(extractJavaMainClassName(source.src)).mapError(_ => CompilationError("No public class with main found"))
      tarStream <- ZIO.succeed(CompressOps.asTarStream(source.src, s"$className.java"))
      _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream)).mapError(_ => RemoteWorkerError(Some("Cant copy archive to container")))
      compileRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(Seq("javac", s"$className.java"), None)).mapError(_ => RemoteWorkerError(Some("Error while compiling with javac ")))
      r <-
        if (compileRes.exitCode.contains(1)) ZIO.fail(CompilationError(compileRes.stdOut))
        else ZIO.succeed(JavaCompilationSuccess(className))
    } yield r

  def runCompiled(compilationSuccess: JavaCompilationSuccess, input: String, maxTime: Long): ZIO[DockerClientContext, Nothing, RawRunResult] = {
    val inputStream = new ByteArrayInputStream(input.getBytes("UTF-8"))
    val runCommand = Seq("java", compilationSuccess.className)

    val run = (for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS).tap(l => printLine(l))
      runRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(runCommand, Some(inputStream))).disconnect
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS).tap(l => printLine(l))
    } yield if (runRes.exitCode.contains(1)) RuntimeError(runRes.stdOut) else Success(runRes.stdOut, endTime - startTime))
      .catchAll(e => ZIO.succeed(UnknownRunError(e.toString)))

    val timeout = for{
      _ <- ZIO.sleep(maxTime.milliseconds)
    } yield TimeLimitExceeded(maxTime)

    for(winner <- run.race(timeout)) yield winner

  }

}
