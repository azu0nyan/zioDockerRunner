package runner

import com.github.dockerjava.api.DockerClient
import runner.DockerOps.Container
import zio.*
import zio.Console.printLine
import zio.stream.{ZSink, ZStream}


object ContainerStream extends ZIOAppDefault {

  //  val s = ZStream.repeatZIO(DockerOps.container("")).buffer(4)

  //  val r:ZStream[Any, Throwable, DockerClient & Container] = {
  //
  //    ZStream.fromIterable(0 to 2).map(x => DockerOps.container("")).foreach(printLine(_))
  //  }

  def makeContainerStream(containerName: String): ZIO[DockerClient, Throwable, ZStream[Any, Throwable, Container]] =
    for (dc <- ZIO.service[DockerClient]) yield {
      ZStream.repeatZIO {
        ZIO.scoped {
          DockerOps.container(containerName).provideSomeLayer(ZLayer.succeed(dc))
        }
      }
    }.take(2).tap(printLine(_))



  //  val result: Task[Unit] = ZStream.repeatZIO(Random.nextInt).foreach(printLine(_))

  def run = ZIO.scoped {
    for {
      _ <- Console.printLine("Begin")
      stream <- makeContainerStream("cont:0.1").provideSomeLayer(DockerOps.clientLayer)
      _ <- stream.run(ZSink.take(2))
    } yield ()
  }

}
