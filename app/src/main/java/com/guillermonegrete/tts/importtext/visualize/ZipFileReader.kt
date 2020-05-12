package com.guillermonegrete.tts.importtext.visualize

import java.io.InputStream

interface ZipFileReader {

    suspend fun getFileStream(filePath: String): InputStream?
}