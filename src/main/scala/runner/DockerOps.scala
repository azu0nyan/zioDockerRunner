package runner

import com.github.dockerjava.api
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.model.{Frame, StreamType}
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import zio.*

import java.io.InputStream

object DockerOps {
  case class Container(id: String)

  case class DockerClientContext(client: DockerClient, container: Container)

  def buildClient: Task[DockerClient] = ZIO.attempt {

    val res = DockerClientBuilder.getInstance
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory()
        .withConnectTimeout(30 * 1000)).build
    println(s"Client open $res")
    res
  }

  def closeClient(c: DockerClient): ZIO[Any, Nothing, Unit] = ZIO.attempt {
    println(s"Client close $c")
    c.close()
  }.catchAll(t => ZIO.logErrorCause("Exception when closing client", Cause.fail(t)))

  def client: ZIO[Scope, Throwable, DockerClient] =
    ZIO.acquireRelease(buildClient)(closeClient)

  def clientLayer: ZLayer[Scope, Throwable, DockerClient] =
    ZLayer.fromZIO(client)

  def clientLayerScooped: ZLayer[Any, Throwable, DockerClient] =
    ZLayer.scoped(client)

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
      ZIO.attempt {
        println(s"cont stop $container")
        dc.killContainerCmd(container.id).exec()
      }.catchAll(t => ZIO.logErrorCause("Exception when killing container", Cause.fail(t)))
    }

  def container(containerName: String): ZIO[DockerClient & Scope, Throwable, Container] =
    ZIO.acquireRelease(makeContainer(containerName))(killContainer)

  def containerLayerScoped(containerName: String): ZLayer[DockerClient, Throwable, Container] =
    ZLayer.scoped(container(containerName))

  def containerLayer(containerName: String): ZLayer[DockerClient & Scope, Throwable, Container] =
    ZLayer.fromZIO(container(containerName))


  def dockerClientContext(containerName: String): ZIO[Scope, Throwable, DockerClientContext] =
      for {
        cl <- client
        cont <- container(containerName).provideSomeLayer(ZLayer.succeed(cl))
      } yield DockerClientContext(cl, cont)

  def dockerClientContextScoped(containerName: String): ZLayer[Any, Throwable,  DockerClientContext] =
    ZLayer.scoped(dockerClientContext(containerName))

  def doInContainer[R: Tag, E: Tag, A: Tag](containerName: String)
                                           (program: ZIO[DockerClientContext & R, E, A]): ZIO[DockerClient & R, E | Throwable, A] =
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
  def copyArchiveToContainer(params: CopyArchiveToContainerParams): ZIO[DockerClientContext, Throwable, Unit] =
    for {
      context <- ZIO.service[DockerClientContext]
      _ <- ZIO.attempt {
        context.client.copyArchiveToContainerCmd(context.container.id)
          .withRemotePath(params.path)
          .withTarInputStream(params.tarStream)
          .exec()
      } //.catchAll(t => ZIO.logErrorCause("Exception when coping archive to container", Cause.fail(t)))
    } yield ()


  case class ExecuteCommandResult(exitCode: Option[Long], stdOut: String, stdErr: String)
  case class ExecuteCommandParams(cmd: Seq[String], input: Option[InputStream] = None)
  def executeCommandInContainer(params: ExecuteCommandParams): ZIO[DockerClientContext, Throwable, ExecuteCommandResult] =
    for {
      context <- ZIO.service[DockerClientContext]
      res <- ZIO.attempt(executeCommandInContainer(context, params))
    } yield res

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
