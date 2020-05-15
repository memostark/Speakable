package com.guillermonegrete.tts.importtext.visualize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.zip.ZipInputStream

class DefaultZipFileReader(inputStream: InputStream?): ZipFileReader {

    private val byteStream: ByteArrayInputStream
    private var shouldResetStream = false

    init {
        val baos = ByteArrayOutputStream()
        IOUtils.copy(inputStream, baos)
        val bytes = baos.toByteArray()
        byteStream = ByteArrayInputStream(bytes)
    }

    override suspend fun getFileStream(filePath: String): InputStream?{
        return withContext(Dispatchers.IO){
            val zipStream: ZipInputStream?

            if(shouldResetStream) byteStream.reset()
            shouldResetStream = true
            try {
                zipStream = ZipInputStream(BufferedInputStream(byteStream))
                var zipEntry = zipStream.nextEntry

                while (zipEntry != null){
                    if(filePath == zipEntry.name) return@withContext zipStream

                    zipStream.closeEntry()
                    zipEntry = zipStream.nextEntry
                }
            }catch (e: IOException){
                println("Error opening file $filePath: $e")
            }
            return@withContext null
        }
    }

    override suspend fun getAllReaders(): Map<String, StringReader> {
        return withContext(Dispatchers.IO){
            val zipStream: ZipInputStream?

            if(shouldResetStream) byteStream.reset()
            shouldResetStream = true

            try {
                zipStream = ZipInputStream(BufferedInputStream(byteStream))
                val streamMap = mutableMapOf<String, StringReader>()

                zipStream.use {
                    var zipEntry = it.nextEntry

                    while (zipEntry != null){
                        val text = it.bufferedReader().readText()
                        streamMap[zipEntry.name] = StringReader(text)

                        it.closeEntry()
                        zipEntry = it.nextEntry
                    }
                }

                return@withContext streamMap
            }catch (e: IOException){
                println("Error opening zip file: $e")
            }
            return@withContext mapOf<String, StringReader>()
        }
    }
}