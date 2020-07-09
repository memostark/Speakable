package com.guillermonegrete.tts.importtext.visualize

import androidx.annotation.VisibleForTesting
import java.io.InputStream
import java.io.StringReader

class FakeZipFileReader: ZipFileReader {

    private val fileStreamService: LinkedHashMap<String, InputStream> = LinkedHashMap()

    override suspend fun getFileStream(filePath: String): InputStream? {
        return fileStreamService[filePath]
    }

    override suspend fun getAllReaders(): Map<String, StringReader> {
        return fileStreamService.mapValues{ entry ->
            StringReader(entry.value.bufferedReader().use { it.readText() })
        }
    }

    @VisibleForTesting
    fun addFileStream(path: String, stream: InputStream){
        fileStreamService[path] = stream
    }

    override suspend fun createFileFolder(path: String) {}

    override suspend fun saveCoverBitmap(coverPath: String, outputDir: String) {}
}