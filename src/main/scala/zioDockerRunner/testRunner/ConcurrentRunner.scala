package zioDockerRunner.testRunner

import zio.*
import zio.stream.ZStream
import zioDockerRunner.dockerIntegration.DockerOps.DockerClientContext

case class ConcurrentRunner(queue: Queue[CompileAndRunMultiple]) {
  
//  def addTask:ZIO[Any, ]

//  val runtime = Runtime.default



//  val stream = ZStream.async[Any, Nothing, CompileAndRunMultiple]{ cb =>
//
//  }
/*
  // Asynchronous Callback-based API
  def registerCallback(
                        name: String,
                        onEvent: CompileAndRunMultiple => Unit,
                      ): Unit = ???

  // Lifting an Asynchronous API to ZStream
  val stream = ZStream.async[Any, Throwable, CompileAndRunMultiple] { cb =>
    registerCallback(
      "foo",
      event => cb(ZIO.succeed(Chunk(event))),
    )
  }

  /**external api*/
  def submitForProcessing(compileAndRunMultiple: CompileAndRunMultiple): Promise[Nothing, CompileAndRunMultipleResult] = ???
*/
}
