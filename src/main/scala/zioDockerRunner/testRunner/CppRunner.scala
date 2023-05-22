package zioDockerRunner.testRunner

import zio.*
import zioDockerRunner.dockerIntegration.DockerOps
import zioDockerRunner.testRunner.CompileResult.CppCompilationSuccess

given CppRunner: LanguageRunner[ProgrammingLanguage.Cpp.type] with {
  override type CompilationSuccessL = CppCompilationSuccess
  override def compile(source: ProgramSource): ZIO[DockerOps.DockerClientContext, CompilationFailure, CppCompilationSuccess] = ???
  override def runCompiled(compilationSuccess: CppCompilationSuccess, input: String, maxTime: Long): ZIO[DockerOps.DockerClientContext, Nothing, RawRunResult] = ???
}