package com.guillermonegrete.tts.importtext.visualize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt


class DefaultZipFileReader(inputStream: InputStream?, context: Context): ZipFileReader {

    private val byteStream: ByteArrayInputStream
    private var shouldResetStream = false

    private val rootDir: File
    private val filesDir
        get() = File(rootDir.absolutePath + File.separator + volumes_folder)

    init {
        val baos = ByteArrayOutputStream()
        IOUtils.copy(inputStream, baos)
        val bytes = baos.toByteArray()
        byteStream = ByteArrayInputStream(bytes)

        // Initialize files dir
        val state = Environment.getExternalStorageState()
        rootDir = if (Environment.MEDIA_MOUNTED == state) {

            val dirs = ContextCompat.getExternalFilesDirs(context, null)
            // Dirs[0] ==> (emulated)
            // Dirs[1] ==> (SD card)
            val dir = if(dirs.size == 2) dirs[1] else dirs[0]
            dir
        } else {
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

        val dir = File(rootDir.absolutePath + File.separator + volumes_folder, path)
        if(!dir.exists()) dir.mkdirs()
    }

    override suspend fun saveCoverBitmap(coverPath: String, outputDir: String){

        var bitmap: Bitmap? = null

        val absOutputDir = File(filesDir, outputDir)

        val file = File(absOutputDir, "cover.png")
        if(!file.exists()){
            val stream = getFileStream(coverPath)
            if(stream != null){

                bitmap = BitmapFactory.decodeStream(stream)
                file.outputStream().use {outStream ->
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 85, outStream)
                    outStream.flush()
                }
            }
        }

        val thumbFile = File(absOutputDir, "cover_thumbnail.png")

        if(!thumbFile.exists()){

            if(bitmap == null) {
                val stream = getFileStream(coverPath)
                if(stream != null) bitmap = BitmapFactory.decodeStream(stream)
            }

            if(bitmap != null){

                // Calculate new dimension keeping aspect ratio
                val aspectRatio: Float = bitmap.width / bitmap.height.toFloat()
                val width = 150
                val height = (width / aspectRatio).roundToInt()
                val thumbBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

                thumbFile.outputStream().use {outStream ->
                    thumbBitmap.compress(Bitmap.CompressFormat.PNG, 85, outStream)
                    outStream.flush()
                }
            }
        }
    }

    companion object{
        const val volumes_folder = "volumes"
    }
}