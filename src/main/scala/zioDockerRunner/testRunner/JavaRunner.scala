package zioDockerRunner.testRunner

import zioDockerRunner.dockerIntegration.DockerOps.{CopyArchiveToContainerParams, DockerClientContext, ExecuteCommandParams, RunningContainerFailure}
import CompileResult.{CompilationError, JavaCompilationSuccess}
import RunResult.{RuntimeError, SuccessffulRun, TimeLimitExceeded, UnknownRunError}
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

  def compile(source: ProgramSource): ZIO[DockerClientContext, RunningContainerFailure | CompilationFailure,  JavaCompilationSuccess] =
    for {
      className <- ZIO.fromOption(extractJavaMainClassName(source.src)).mapError(_ => CompilationError("No public class with main found"))
      tarStream <- ZIO.succeed(CompressOps.asTarStream(source.src, s"$className.java"))
      _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream))
      compileRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(Seq("javac", s"$className.java"), None))
      r <-
        if (compileRes.exitCode.contains(1)) ZIO.fail(CompilationError(compileRes.stdOut))
        else ZIO.succeed(JavaCompilationSuccess(className))
    } yield r

  def runCompiled(compilationSuccess: JavaCompilationSuccess, input: String, maxTime: Long): ZIO[DockerClientContext, RunningContainerFailure, RawRunResult] = {
    val inputStream = new ByteArrayInputStream(input.getBytes("UTF-8"))
    val runCommand = Seq("java", compilationSuccess.className)

    val run: ZIO[DockerClientContext, RunningContainerFailure, RawRunResult] = (for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      runRes <- DockerOps.executeCommandInContainer(ExecuteCommandParams(runCommand, Some(inputStream))).disconnect
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    } yield if (runRes.exitCode.contains(1)) RuntimeError(runRes.stdOut) else SuccessffulRun(runRes.stdOut, endTime - startTime))


    run.timeoutTo[RawRunResult](TimeLimitExceeded(maxTime))(x => x)(maxTime.milliseconds)


//    val timeout = for{
//      _ <- ZIO.sleep(maxTime.milliseconds)
//    } yield TimeLimitExceeded(maxTime)
//
//    for(winner <- run.race(timeout)) yield winner

  }

}
