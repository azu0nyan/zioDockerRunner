package runner

import runner.DockerOps.{CopyArchiveToContainerParams, ExecuteCommandParams, ExecuteCommandResult}
import zio.test.*
import zio.test.Assertion.*

import java.io.ByteArrayInputStream
import scala.io.Source

object DockerOpsTests extends ZIOSpecDefault {

  def spec = suite("DockerOpsSpec") {

    test("Compiling java program") {
      val javaFileText = Source.fromResource("Main.java").mkString("")
      val tarStream = CompressOps.asTarStream(javaFileText, "Main.java")

      val testContainerName = "cont:0.1"
      val compileCommand = Seq("javac", "Main.java")
      val runCommand = Seq("java", "Main")
      val input = new ByteArrayInputStream("2".getBytes("UTF-8"))

      val toRunInContainer = for {
        _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream))
        res <- DockerOps.executeCommandInContainer(ExecuteCommandParams(compileCommand, None))
        res2 <- DockerOps.executeCommandInContainer(ExecuteCommandParams(runCommand, Some(input)))
      } yield (res, res2)

      val res = for (r <- DockerOps.doInContainer(testContainerName)(toRunInContainer).provideLayer(DockerOps.clientLayer)) yield r

      assertZIO(res)(
        hasField("firstResult", (x: (ExecuteCommandResult, ExecuteCommandResult)) => x._1, equalTo(ExecuteCommandResult(Some(0), "", ""))) &&
          hasField("secondResult", (x: (ExecuteCommandResult, ExecuteCommandResult)) => x._2, equalTo(ExecuteCommandResult(Some(0), "RESPONSE2\n", "")))

      )

    }


  }


}
