package com.guillermonegrete.tts.importtext.visualize

import java.io.InputStream
import java.io.StringReader

interface ZipFileReader {

    suspend fun getFileStream(filePath: String): InputStream?

    suspend fun getAllReaders(): Map<String, StringReader>

    suspend fun createFileFolder(path: String)
}