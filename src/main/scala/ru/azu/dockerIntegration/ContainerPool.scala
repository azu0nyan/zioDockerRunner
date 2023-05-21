package ru.azu.dockerIntegration

import com.github.dockerjava.api.DockerClient
import zio.*
object ContainerPool extends ZIOAppDefault {
//  def makePool = ZPool.make(ZIO.succeed(2).tap(Console.printLine(_)), 10)
  def makePool: ZIO[Scope, Nothing, ZPool[Throwable, DockerClient]] = ZPool.make(DockerOps.client, 3)

  def run = for{
    pool <- makePool
    dc1 <- pool.get
    _ <- Console.printLine(s"invalidating:$dc1")
    _ <- pool.invalidate(dc1)
    _ <- ZIO.sleep(1.seconds)
    _ <- Console.printLine(s"after sleep")
    dc2 <- pool.get
    _ <- Console.printLine(s"dc:$dc2")
  } yield ()


}
