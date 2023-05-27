package zioDockerRunner.dockerIntegration

import com.github.dockerjava.api
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.model.{Frame, StreamType}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder, DockerClientConfig}
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import zio.*
import zioDockerRunner.dockerIntegration.DockerOps.DockerFailure.*

import java.io.InputStream

object DockerOps {

  sealed trait DockerFailure
  sealed trait RunningContainerFailure extends DockerFailure
  case object DockerFailure {
    case object UnknownDockerFailure extends DockerFailure
    case object WorkQueueIsFull extends DockerFailure
    final case class CantCreateClient(errorMessage: Option[String] = None) extends DockerFailure
    final case class CantCreateContainer(errorMessage: Option[String] = None) extends DockerFailure

    final case class CantExecuteCommand(errorMessage: Option[String] = None) extends RunningContainerFailure
    final case class CantCopyToContainer(errorMessage: Option[String] = None) extends RunningContainerFailure
  }


  case class Container(id: String)
  case class DockerClientContext(client: DockerClient, container: Container)

  def buildClient(config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): Task[DockerClient] = ZIO.attemptBlocking {

    val res = DockerClientBuilder.getInstance(config)
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory()
        .withConnectTimeout(30 * 1000)).build()
    println(s"Client open $res")
    res
  }

  def closeClient(c: DockerClient): ZIO[Any, Nothing, Unit] = ZIO.attemptBlocking {
    println(s"Client close $c")
    c.close()
  }.catchAll(t => ZIO.logErrorCause("Exception when closing client", Cause.fail(t)))

  def client(config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): ZIO[Scope, CantCreateClient, DockerClient] =
    ZIO.acquireRelease(buildClient(config))(closeClient).mapError(t => CantCreateClient(Some(t.toString)))

  def clientLayer(config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): ZLayer[Scope, CantCreateClient, DockerClient] =
    ZLayer.fromZIO(client(config))

  def clientLayerScooped(config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): ZLayer[Any, CantCreateClient, DockerClient] =
    ZLayer.scoped(client(config))

  def makeContainer(containerName: String): ZIO[DockerClient, Throwable, Container] =
    for {
      dc <- ZIO.service[DockerClient]
    } yield {

      val createContainerCmd = dc.createContainerCmd(containerName).withTty(true)
      val createContainerResponse = createContainerCmd.exec()
      val containerID = createContainerResponse.getId
      dc.startContainerCmd(containerID).exec()
      println(s"cont start $containerID")
      Container(containerID)
    }



  def killContainer(container: Container): ZIO[DockerClient, Nothing, Any] =
    ZIO.service[DockerClient].flatMap { dc =>
      ZIO.attemptBlocking {
        println(s"cont stop $container")
        dc.killContainerCmd(container.id).exec()
      }.catchAll(t => ZIO.logErrorCause("Exception when killing container", Cause.fail(t)))
    }

  def container(containerName: String): ZIO[DockerClient & Scope, CantCreateContainer, Container] =
    ZIO.acquireRelease(makeContainer(containerName))(killContainer).mapError(t => CantCreateContainer(Some(t.toString)))

  def containerLayerScoped(containerName: String): ZLayer[DockerClient, CantCreateContainer, Container] =
    ZLayer.scoped(container(containerName))

  def containerLayer(containerName: String): ZLayer[DockerClient & Scope, CantCreateContainer, Container] =
    ZLayer.fromZIO(container(containerName))

  //The following import might make progress towards fixing the problem: IDK WHY
//  import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable
  def dockerClientContext(containerName: String, config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): ZIO[Scope, CantCreateClient | CantCreateContainer, DockerClientContext] =
      for {
        cl <- client(config)
        cont <- container(containerName).provideSomeLayer(ZLayer.succeed(cl))
      } yield DockerClientContext(cl, cont)

  def dockerClientContextScoped(containerName: String, config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()): ZLayer[Any, CantCreateClient | CantCreateContainer,  DockerClientContext] =
    ZLayer.scoped(dockerClientContext(containerName, config))

  def doInContainer[R: Tag, E: Tag, A: Tag](containerName: String)
                                           (program: ZIO[DockerClientContext & R, E, A]): ZIO[DockerClient & R, E | DockerFailure, A] =
    ZIO.scoped {
      for {
        cl <- ZIO.service[DockerClient]
        cont <- container(containerName)
        res <- program.provideSomeLayer(ZLayer.succeed(DockerClientContext(cl, cont)))
      } yield res
    }

  //    ZIO.scoped {
  //      container(containerName).flatMap(c => program.provideSomeLayer(ZLayer.succeed(c)))
  //    }

  case class CopyArchiveToContainerParams(path: String = "/", tarStream: InputStream)
  def copyArchiveToContainer(params: CopyArchiveToContainerParams): ZIO[DockerClientContext, CantCopyToContainer, Unit] =
    (for {
      context <- ZIO.service[DockerClientContext]
      _ <- ZIO.attemptBlocking {
        context.client.copyArchiveToContainerCmd(context.container.id)
          .withRemotePath(params.path)
          .withTarInputStream(params.tarStream)
          .exec()
      } //.catchAll(t => ZIO.logErrorCause("Exception when coping archive to container", Cause.fail(t)))
    } yield ()).mapError(t => CantCopyToContainer(Some(t.toString)))


  case class ExecuteCommandResult(exitCode: Option[Long], stdOut: String, stdErr: String)
  case class ExecuteCommandParams(cmd: Seq[String], input: Option[InputStream] = None)
  def executeCommandInContainer(params: ExecuteCommandParams): ZIO[DockerClientContext, CantExecuteCommand, ExecuteCommandResult] =
    (for {
      context <- ZIO.service[DockerClientContext]
      res <- ZIO.attemptBlocking(executeCommandInContainer(context, params))
    } yield res).mapError(t => CantExecuteCommand(Some(t.toString)))

  //  .catchAll(t =>
  //    ZIO.logErrorCause("Exception when executing command in container client", Cause.fail(t))
  //  )

  def executeCommandInContainer(context: DockerClientContext, params: ExecuteCommandParams): ExecuteCommandResult = {
    println(s"Executing ${params.cmd}")

    val command = context.client.execCreateCmd(context.container.id)
      .withCmd(params.cmd: _ *)
      .withAttachStdin(true)
      .withAttachStderr(true)
      .withAttachStdout(true)
      .exec()

    val out = new StringBuilder()
    val err = new StringBuilder()

    val execution = context.client.execStartCmd(command.getId)
      .withStdIn(params.input.orNull)
      .exec(new ResultCallbackTemplate {
        override def onNext(f: Frame): Unit = {
          f.getStreamType match
            case StreamType.STDOUT =>
              out.append(new String(f.getPayload, "UTF-8"))
            case StreamType.STDERR =>
              err.append(new String(f.getPayload, "UTF-8"))
            case StreamType.STDIN =>
            case StreamType.RAW =>
        }
      })

    execution.awaitCompletion()

    val retCode = context.client.inspectExecCmd(command.getId).exec().getExitCodeLong

    val res = ExecuteCommandResult(Option(retCode), out.toString(), err.toString())
    println(res)
    res
  }



  /*
  def executeCommandInContainer(cmd: Seq[String], input: Option[InputStream] = None)(cont: Container)
                               (implicit dc: DockerClient): ExecuteCommandResult = {

  }
*/

}
