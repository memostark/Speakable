package com.guillermonegrete.tts.importtext.visualize

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.zip.ZipInputStream

class DefaultZipFileReader(inputStream: InputStream?, context: Context): ZipFileReader {

    private val byteStream: ByteArrayInputStream
    private var shouldResetStream = false

    private val rootDir: File?

    init {
        val baos = ByteArrayOutputStream()
        IOUtils.copy(inputStream, baos)
        val bytes = baos.toByteArray()
        byteStream = ByteArrayInputStream(bytes)

        // Initialize files dir
        val state = Environment.getExternalStorageState()
        rootDir = if (Environment.MEDIA_MOUNTED == state) {
            println("Creating external dir")

            val dirs = ContextCompat.getExternalFilesDirs(context, null)
            // Dirs[0] ==> (emulated)
            // Dirs[1] ==> (SD card)
            val dir = if(dirs.size == 2) dirs[1] else dirs[0]
            println("External dirs: ${dirs.size}, $dir.")
            dir
        } else {
            println("Creating internal dir")
            context.filesDir
        }
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

    override suspend fun createFileFolder(path: String) {
        rootDir ?: return

        val dir = File(rootDir.absolutePath + File.separator + volumes_folder, path)
        if(!dir.exists()) dir.mkdirs()
    }

    companion object{
        const val volumes_folder = "volumes"
    }
}