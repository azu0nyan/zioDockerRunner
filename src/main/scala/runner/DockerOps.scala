package runner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import zio.*

object DockerOps {
  type ContainerRef = String


  def buildClient: Task[DockerClient] = ZIO.attempt {
    DockerClientBuilder.getInstance
      .withDockerCmdExecFactory(new NettyDockerCmdExecFactory()
        .withConnectTimeout(30 * 1000))
      .build
  }

  def closeClient(c: DockerClient): Task[Unit] = ZIO.attempt {
    c.close()
  }

  def makeContainer(containerName: String)(dc: DockerClient): Task[ContainerRef] = ZIO.attempt {
    val createContainerCmd = dc.createContainerCmd(containerName).withTty(true)
    val createContainerResponse = createContainerCmd.exec()
    val container = createContainerResponse.getId
    dc.startContainerCmd(container).exec()
    container
  }

  def killContainer(container: ContainerRef)(dc: DockerClient): Task[Unit] = ZIO.attempt {
    dc.killContainerCmd(container).exec()
  }

}
