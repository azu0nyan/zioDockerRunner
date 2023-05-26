package zioDockerRunner.testRunner

import zio.*
import zio.stream.ZStream
import zioDockerRunner.dockerIntegration.DockerOps
import zioDockerRunner.dockerIntegration.DockerOps.DockerFailure.WorkQueueIsFull
import zioDockerRunner.dockerIntegration.DockerOps.{DockerClientContext, DockerFailure}
import zioDockerRunner.testRunner.ConcurrentRunner.*

object ConcurrentRunner {
  case class ConcurrentRunnerConfig(
                                     fibersMax: Int,
                                     containerName: String
                                   )

  type TaskResultPromise = Promise[DockerFailure, CompileAndRunMultipleResult]
  case class TaskAndPromise(task: CompileAndRunMultiple, promise: TaskResultPromise)


}

case class ConcurrentRunner(queue: Queue[TaskAndPromise], config: ConcurrentRunnerConfig) {

  //  def addTaskExternal(crm: CompileAndRunMultiple): scala.concurrent.Future[DockerFailure | TaskResultPromise] =
  //    addTask(crm).toFuture.map(_.)

  def addTask(crm: CompileAndRunMultiple): UIO[TaskResultPromise] =
    for {
      prom <- Promise.make[DockerFailure, CompileAndRunMultipleResult]
      succ <- queue.offer(TaskAndPromise(crm, prom))
      _ <- ZIO.when(!succ)(prom.fail(WorkQueueIsFull))
    } yield prom


  //todo mb ZIO.scoped here
  def runTask(taskAndPromise: TaskAndPromise): ZIO[Any, Nothing, Unit] =
    (for {
      res <- Runner.compileAndRunMultiple(taskAndPromise.task).provideSomeLayer(DockerOps.dockerClientContextScoped(config.containerName))
      _ <- taskAndPromise.promise.succeed(res)
    } yield ())
      .catchAll(e => for {
        _ <- taskAndPromise.promise.fail(e)
      } yield ())

  def fiber: ZIO[Any, Nothing, Unit] =
    (for {
      taskAndPromise <- queue.take
      _ <- runTask(taskAndPromise)
    } yield ()).forever

  def run: ZIO[Any, Nothing, Unit] =
    ZIO.foreachDiscard(0 until config.fibersMax) { id =>
      fiber.fork
    }


}
