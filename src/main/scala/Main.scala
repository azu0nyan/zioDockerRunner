import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.{ResultCallback, ResultCallbackTemplate}
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import runner.{CompressOps, ContainerOps}

import java.io.{ByteArrayInputStream, Closeable}
import java.util.stream.Collectors
import scala.io.Source
import scala.jdk.CollectionConverters.*

object Main extends App{
  implicit val dc: DockerClient = DockerClientBuilder
    .getInstance
    .withDockerCmdExecFactory(new NettyDockerCmdExecFactory()
      .withConnectTimeout(30 * 1000))
    .build
  

  val createContainer = dc.createContainerCmd("cont:0.1") .withTty(true)

  val javaFileText = Source.fromResource("Main.java").mkString("")
  println(javaFileText)

  val tarStream =  CompressOps.asTarStream(javaFileText, "Main.java")
//  val tarStream = getClass.getClassLoader.getResourceAsStream("Main.tar")

  val input = new ByteArrayInputStream("2".getBytes("UTF-8"))


  ContainerOps.doInNewContainer(createContainer){ cont =>
    dc.copyArchiveToContainerCmd(cont)
      .withRemotePath("/")
      .withTarInputStream(tarStream)
      .exec()

    ContainerOps.executeCommandInContainer(Seq("javac", "Main.java"))(cont)
    ContainerOps.executeCommandInContainer(Seq("java", "Main"), Some(input))(cont)

    scala.io.StdIn.readLine()
  }

  println(dc.listContainersCmd().exec())

  System.exit(0)

}
