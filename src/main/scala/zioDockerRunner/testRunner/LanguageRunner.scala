package zioDockerRunner.testRunner

import zioDockerRunner.dockerIntegration.DockerOps.DockerClientContext
import zio._



trait LanguageRunner[L <: ProgrammingLanguage] {
  type CompilationSuccessL <: CompilationSuccess[L]

  def compile(source: ProgramSource): ZIO[DockerClientContext, CompilationFailure, CompilationSuccessL]

  def runCompiled(compilationSuccess: CompilationSuccessL, input: String, maxTime: Long): ZIO[DockerClientContext, Nothing, RunResult]

}
