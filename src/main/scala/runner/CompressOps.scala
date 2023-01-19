package runner

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.io.output.ByteArrayOutputStream

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.charset.Charset


object CompressOps {
  def asTarStream(content:String, filename:String ): InputStream = {
    val res = new ByteArrayOutputStream()
    val tarOut = new TarArchiveOutputStream(res)


    val data = content.getBytes(Charset.forName("UTF-8"))
    val entry = new TarArchiveEntry(filename)
    entry.setSize(data.length)

    tarOut.putArchiveEntry(entry)
    tarOut.write(data)
    tarOut.closeArchiveEntry()

    tarOut.finish()

    res.toInputStream
    //new ByteArrayInputStream(res.toByteArray)
  }
}
