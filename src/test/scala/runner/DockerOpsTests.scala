package runner

import runner.DockerOps.{CopyArchiveToContainerParams, ExecuteCommandParams, ExecuteCommandResult}
import validator.Runner
import validator.Runner.ProgramSource
import zio.ZLayer
import zio.test.*
import zio.test.Assertion.*

import java.io.ByteArrayInputStream
import scala.io.Source

object DockerOpsTests extends ZIOSpecDefault {
  val testContainerName = "cont:0.1"


  val t1 = test("Compiling java program without Runner") {
    val javaFileText = Source.fromResource("CorrectJavaProgramWithMainClass.java").mkString("")
    val tarStream = CompressOps.asTarStream(javaFileText, "Main.java")

    val failedCommand = Seq("javac", "Main.jav")
    val compileCommand = Seq("javac", "Main.java")
    val runCommand = Seq("java", "Main")
    val input = new ByteArrayInputStream("2".getBytes("UTF-8"))

    val toRunInContainer = for {
      _ <- DockerOps.copyArchiveToContainer(CopyArchiveToContainerParams("/", tarStream))
      res <- DockerOps.executeCommandInContainer(ExecuteCommandParams(compileCommand, None))
      res2 <- DockerOps.executeCommandInContainer(ExecuteCommandParams(runCommand, Some(input)))
      res3 <- DockerOps.executeCommandInContainer(ExecuteCommandParams(failedCommand, None))
    } yield (res, res2, res3)

    val res = for (r <- DockerOps.doInContainer(testContainerName)(toRunInContainer).provideLayer(DockerOps.clientLayerScooped)) yield r

    assertZIO(res)(
      hasField("firstResult", (x: (ExecuteCommandResult, ExecuteCommandResult, ExecuteCommandResult)) => x._1, equalTo(ExecuteCommandResult(Some(0), "", ""))) &&
        hasField("secondResult", (x: (ExecuteCommandResult, ExecuteCommandResult, ExecuteCommandResult)) => x._2, equalTo(ExecuteCommandResult(Some(0), "RESPONSE2\n", ""))) &&
        hasField("thirdCode", (x: (ExecuteCommandResult, ExecuteCommandResult, ExecuteCommandResult)) => x._3.exitCode, equalTo(Some(1))) &&
        hasField("thirdErrorText", (x: (ExecuteCommandResult, ExecuteCommandResult, ExecuteCommandResult)) => x._3.stdErr.toSeq, isNonEmpty)
    )

  }

  val t2= test("Compiling incorrect java program with Runner") {
    val javaFileText = Source.fromResource("IncorrectJavaProgram.java").mkString("")

    val res = for {
      r <- Runner.compileJava(ProgramSource(javaFileText)).provideSomeLayer(DockerOps.dockerClientContextScoped(testContainerName))
    } yield r

    println(res)
//    assertM
    assertZIO(res)(failsCause())
  }

  def spec = suite("DockerOpsSpec")(
    t1, t2
  )


}
