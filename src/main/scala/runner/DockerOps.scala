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
  type Container = String

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
      val container = createContainerResponse.getId
      dc.startContainerCmd(container).exec()
      println(s"cont start $container")
      container
    }


  def killContainer(container: Container): ZIO[DockerClient, Nothing, Any] =
    ZIO.service[DockerClient].flatMap { dc =>
      ZIO.attempt {
        println(s"cont stop $container")
        dc.killContainerCmd(container).exec()
      }.catchAll(t => ZIO.logErrorCause("Exception when killing container", Cause.fail(t)))
    }

  def container(containerName: String): ZIO[DockerClient & Scope, Throwable, Container] =
    ZIO.acquireRelease(makeContainer(containerName))(killContainer)

  def containerLayerScoped(containerName: String): ZLayer[DockerClient, Throwable, Container] =
    ZLayer.scoped(container(containerName))

  def containerLayer(containerName: String): ZLayer[DockerClient & Scope, Throwable, Container] =
    ZLayer.fromZIO(container(containerName))

  def doInContainer[R, E, A](containerName: String)(program: ZIO[DockerClient & Container & R, E, A]): ZIO[DockerClient & R, E | Throwable, A] =
    ZIO.scoped {
      container(containerName).flatMap(c => program.provideSomeLayer(ZLayer.succeed(c)))
    }

  case class CopyArchiveToContainerParams(path: String = "/", tarStream: InputStream)
  def copyArchiveToContainer(params: CopyArchiveToContainerParams): ZIO[DockerClient & Container, Throwable, Unit] =
    for {
      dc <- ZIO.service[DockerClient]
      c <- ZIO.service[Container]
      _ <- ZIO.attempt {
        dc.copyArchiveToContainerCmd(c)
          .withRemotePath(params.path)
          .withTarInputStream(params.tarStream)
          .exec()
      } //.catchAll(t => ZIO.logErrorCause("Exception when coping archive to container", Cause.fail(t)))
    } yield ()


  case class ExecuteCommandResult(exitCode: Option[Long], stdOut: String, stdErr: String)
  case class ExecuteCommandParams(cmd: Seq[String], input: Option[InputStream] = None)
  def executeCommandInContainer(params: ExecuteCommandParams): ZIO[DockerClient & Container, Throwable, ExecuteCommandResult] =
    for {
      dc <- ZIO.service[DockerClient]
      c <- ZIO.service[Container]
      res <- ZIO.attempt(executeCommandInContainer(dc, c, params))
    } yield res

  //  .catchAll(t =>
  //    ZIO.logErrorCause("Exception when executing command in container client", Cause.fail(t))
  //  )

  def executeCommandInContainer(dc: DockerClient, c: Container, params: ExecuteCommandParams): ExecuteCommandResult = {
    println(s"Executing ${params.cmd}")

    val command = dc.execCreateCmd(c)
      .withCmd(params.cmd: _ *)
      .withAttachStdin(true)
      .withAttachStderr(true)
      .withAttachStdout(true)
      .exec()

    val out = new StringBuilder()
    val err = new StringBuilder()

    val execution = dc.execStartCmd(command.getId)
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

    val retCode = dc.inspectExecCmd(command.getId).exec().getExitCodeLong

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
