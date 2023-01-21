package runner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import runner.DockerOps.Container
import zio.*

import java.io.IOException

object ZContainerOps extends ZIOAppDefault {
  def run = myApp
  
  type Container = String
  
  //    val myApp: ZIO[Any, IOException, Unit] = Console.printLine("Hello, World!")

  val toRun:ZIO[DockerClient & Container , Throwable, Any] = Console.printLine("running")

  val myApp: ZIO[Any, Throwable, Unit] = for {
    _ <- DockerOps.doInContainer("cont:0.1")(toRun).provideLayer(DockerOps.clientLayer)
  } yield ()



 
 

}
