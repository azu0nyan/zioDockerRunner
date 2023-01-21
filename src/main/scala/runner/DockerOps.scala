package runner

import com.github.dockerjava.api
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import zio.*

object DockerOps {
  type Container = String

  def buildClient: Task[DockerClient] = ZIO.attempt {
    println("Client open")
    DockerClientBuilder.getInstance
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory()
        .withConnectTimeout(30 * 1000)).build
  }

  def closeClient(c: DockerClient): ZIO[Any, Nothing, Unit] = ZIO.attempt {
    println("Client close")
    c.close()
  }.catchAll(t => ZIO.logErrorCause("Exception when closing client", Cause.fail(t)))

  def client: ZIO[Scope, Throwable, DockerClient] =
    ZIO.acquireRelease(buildClient)(closeClient)

  def clientLayer: ZLayer[Any, Throwable, DockerClient] =
    ZLayer.scoped(client)
  def makeContainer(containerName: String): ZIO[DockerClient, Throwable, Container] =
    for {
      dc <- ZIO.service[DockerClient]
    } yield {
      println("cont start")
      val createContainerCmd = dc.createContainerCmd(containerName).withTty(true)
      val createContainerResponse = createContainerCmd.exec()
      val container = createContainerResponse.getId
      dc.startContainerCmd(container).exec()
      container
    }


  def killContainer(container: Container) : ZIO[DockerClient, Nothing, Any] =
    ZIO.service[DockerClient].flatMap{ dc =>
      ZIO.attempt {
        println("cont stop")
        dc.killContainerCmd(container).exec()
      }.catchAll(t => ZIO.logErrorCause("Exception when killing container", Cause.fail(t)))
    }

  def container(containerName: String): ZIO[DockerClient & Scope, Throwable, Container] =
    ZIO.acquireRelease(makeContainer(containerName))(killContainer)

  def containerLayer(containerName: String): ZLayer[DockerClient, Throwable, Container] =
    ZLayer.scoped(container(containerName))


  def doInContainer[R, E, A](containerName: String)(program: ZIO[DockerClient & Container & R, E, A]): ZIO[DockerClient & R, E | Throwable, A] =
    ZIO.scoped {
      container(containerName).flatMap(c => program.provideSomeLayer(ZLayer.succeed(c)))
    }



}
