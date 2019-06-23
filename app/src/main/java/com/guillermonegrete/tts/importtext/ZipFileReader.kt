package com.guillermonegrete.tts.importtext

import org.apache.commons.io.IOUtils
import java.io.*
import java.util.zip.ZipInputStream

class ZipFileReader(inputStream: InputStream?) {

    private val byteStream: ByteArrayInputStream
    private var shouldResetStream = false

    init {
        val baos = ByteArrayOutputStream()
        IOUtils.copy(inputStream, baos)
        val bytes = baos.toByteArray()
        byteStream = ByteArrayInputStream(bytes)
    }

    fun getFileStream(filePath: String): InputStream?{
        val zipStream: ZipInputStream?

        if(shouldResetStream) byteStream.reset()
        shouldResetStream = true
        try {
            zipStream = ZipInputStream(BufferedInputStream(byteStream))
            var zipEntry = zipStream.nextEntry

            while (zipEntry != null){
                if(filePath == zipEntry.name) return zipStream

                zipStream.closeEntry()
                zipEntry = zipStream.nextEntry
            }
        }catch (e: IOException){
            println("Error opening file $filePath: $e")
        }
        return null
    }
}