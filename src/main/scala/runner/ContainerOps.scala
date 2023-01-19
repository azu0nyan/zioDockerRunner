package runner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.{Frame, StreamType}

import java.io.InputStream
import java.nio.charset.Charset


object ContainerOps {
  type Container = String


  //  def executeCommandInContainer[A](): Either[Throwable, A]

  def doInNewContainer[A](cc: CreateContainerCmd)(action: Container => A)(implicit dc: DockerClient): Either[Throwable, A] = {
    val createContainerResponse = cc.exec()
    val container: Container = createContainerResponse.getId
    println(s"Container $container created.")
    try {
      dc.startContainerCmd(container).exec()
      println(s"Container $container started.")
      if (createContainerResponse.getWarnings.nonEmpty) {
        println(createContainerResponse.getWarnings.mkString("WARNINGS(", ", ", ")"))
      }
      Right(action(container))
    } catch {
      case t: Throwable =>
        println(t)
        Left(t)
    } finally {
      dc.killContainerCmd(container).exec()
      println(dc.listContainersCmd().exec())
    }
  }


  case class ExecuteCommandResult(exitCode: Option[Long], stdOut: String, stdErr: String)
  def executeCommandInContainer(cmd: Seq[String], input: Option[InputStream] = None)(cont: Container)
                               (implicit dc: DockerClient):ExecuteCommandResult = {
    println(s"Executing $cmd")

    val command = dc.execCreateCmd(cont)
      .withCmd(cmd: _ *)
      .withAttachStdin(true)
      .withAttachStderr(true)
      .withAttachStdout(true)
      .exec()


    val out = new StringBuilder()
    val err = new StringBuilder()

    val execution = dc.execStartCmd(command.getId)
      .withStdIn(input.orNull)
      .exec(new ResultCallbackTemplate {
      override def onNext(f: Frame): Unit = {
        f.getStreamType match
          case StreamType.STDOUT =>
            out.append(new String(f.getPayload, "UTF-8"))
          case StreamType.STDERR =>
            err.append(new String(f.getPayload, "UTF-8"))
          case StreamType.STDIN =>
          case StreamType.RAW =>
      }
    })

    execution.awaitCompletion()

    val retCode = dc.inspectExecCmd(command.getId).exec().getExitCodeLong

    val res = ExecuteCommandResult(Option(retCode), out.toString(), err.toString())
    println(res)
    res
  }
}
