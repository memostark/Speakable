package com.guillermonegrete.tts.importtext.visualize

import androidx.annotation.VisibleForTesting
import java.io.InputStream

class FakeZipFileReader: ZipFileReader {

    private val fileStreamService: LinkedHashMap<String, InputStream> = LinkedHashMap()

    override suspend fun getFileStream(filePath: String): InputStream? {
        return fileStreamService[filePath]
    }

    @VisibleForTesting
    fun addFileStream(path: String, stream: InputStream){
        fileStreamService[path] = stream
    }
}