package zioDockerRunner.testRunner

import zio.*
import ProgrammingLanguage.*
import zioDockerRunner.dockerIntegration.DockerOps.DockerClientContext
import zioDockerRunner.dockerIntegration.DockerOps.{DockerClientContext, RunningContainerFailure}

object Runner {

  def compileAndRunMultiple(compileAndRunMultiple: CompileAndRunMultiple): ZIO[DockerClientContext, RunningContainerFailure, CompileAndRunMultipleResult] = {
    compileAndRunMultiple.language match
      case Java => JavaRunner.compileAndRunMultiple(compileAndRunMultiple)
      case Cpp => CppRunner.compileAndRunMultiple(compileAndRunMultiple)
      case _ => {
        println(s"LOL")
        ???
      }
    //      case Haskell => compileAndRunMultipleUsing[Haskell.type](compileAndRunMultiple)
    //      case Scala => compileAndRunMultipleUsing[Scala.type](compileAndRunMultiple)
    //      case Kojo => compileAndRunMultipleUsing[Kojo.type](compileAndRunMultiple)
  }

}
