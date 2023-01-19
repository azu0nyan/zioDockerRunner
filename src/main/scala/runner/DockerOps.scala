package runner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import zio.*

object DockerOps {
  type ContainerRef = String

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

  //  def makeContainer(containerName: String)(dc: DockerClient): Task[ContainerRef] = ZIO.attempt {
  //    val createContainerCmd = dc.createContainerCmd(containerName).withTty(true)
  //    val createContainerResponse = createContainerCmd.exec()
  //    val container = createContainerResponse.getId
  //    dc.startContainerCmd(container).exec()
  //    container
  //  }

  def makeContainer(containerName: String): ZIO[DockerClient, Throwable, ContainerRef] =
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

  def killContainer(container: ContainerRef)(dc: DockerClient): ZIO[DockerClient, Nothing, Unit] = ZIO.attempt {
    println("cont stop")
    dc.killContainerCmd(container).exec()
    ()
  }.catchAll(t => ZIO.logErrorCause("Exception when killing container", Cause.fail(t)))


  def container(containerName: String): ZIO[DockerClient & Scope, Throwable, ContainerRef] =
    for {
      cont <- makeContainer(containerName)
    } yield (cont)


//  def containerLayer(containerName: String): ZLayer[DockerClient, Throwable, ContainerRef] =
//    ZLayer.scoped {
//      ZIO.acquireRelease(makeContainer(containerName))(killContainer())
//    }

  //  def containerLayer(containerName: String): ZLayer[DockerClient, Throwable, ContainerRef] =
  //    ZLayer.fromZIO(makeContainer(containerName))
}
