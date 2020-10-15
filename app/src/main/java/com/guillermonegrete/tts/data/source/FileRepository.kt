package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile

interface FileRepository{
    suspend fun getRecentFiles(): List<BookFile>

    suspend fun getFiles(): List<BookFile>

    suspend fun getFile(id: Int): BookFile?

    suspend fun getFile(uri: String): BookFile?

    suspend fun saveFile(file: BookFile)

    suspend fun deleteFile(file: BookFile)
}