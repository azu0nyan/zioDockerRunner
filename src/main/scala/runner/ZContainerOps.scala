package runner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import zio.*

import java.io.IOException

object ZContainerOps extends ZIOAppDefault {
  type Container = String


//  val myApp: ZIO[Any, IOException, Unit] = Console.printLine("Hello, World!")
 /* val myApp: ZIO[Any, Throwable, Unit] = for{
    dc <- DockerOps.buildClient
    cont <- DockerOps.makeContainer("cont:0.1")(dc)
    _ <- Console.printLine(cont)
    _ <- DockerOps.killContainer(cont)(dc)
    _ <- DockerOps.closeClient(dc)
  } yield ()*/
 val myApp: ZIO[Any, Throwable, Unit] = ZIO.scoped {
   for {
     dc <- DockerOps.client
     cont <- DockerOps.container("cont:0.1")
     _ <- Console.printLine("")
     //_ <- DockerOps.killContainer(cont)(dc)
   } yield ()
 }


  def run = myApp


//  val createContainer = dc.createContainerCmd("cont:0.1") .withTty(true)
//  def run = doInNewContainer()


//  def doInNewContainer[A](cc: CreateContainerCmd)(action: Container => A): ZIO.Acquire[DockerClient, Any, Any] =
//    ZIO.acquireReleaseWith[DockerClient, Any, Any] {
//      for {
//        _ <- ZIO.attemptBlocking(cc.exec())
//        s1 <- ZIO.service[DockerClient]
//      } yield ()
//    }

//  def doInNewContainter[A](containerName: String): ZIO.Acquire[DockerClient, Any, Any] =
//    ZIO.ac



}
