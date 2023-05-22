package zioDockerRunner.testRunner

import zio.*
import ProgrammingLanguage.*
import zioDockerRunner.dockerIntegration.DockerOps.DockerClientContext

object Runner {

  def compileAndRunMultiple(compileAndRunMultiple: CompileAndRunMultiple) = {
    compileAndRunMultiple.language match
      case Java => JavaRunner.compileAndRunMultiple(compileAndRunMultiple)
      case Cpp => CppRunner.compileAndRunMultiple(compileAndRunMultiple)
      case _ => ???
    //      case Haskell => compileAndRunMultipleUsing[Haskell.type](compileAndRunMultiple)
    //      case Scala => compileAndRunMultipleUsing[Scala.type](compileAndRunMultiple)
    //      case Kojo => compileAndRunMultipleUsing[Kojo.type](compileAndRunMultiple)
  }

}
