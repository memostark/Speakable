package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile
import kotlinx.coroutines.flow.Flow

interface FileRepository{
    fun getRecentFiles(): Flow<List<BookFile>>

    suspend fun getFiles(): List<BookFile>

    suspend fun getFile(id: Int): BookFile?

    suspend fun getFile(uri: String): BookFile?

    suspend fun saveFile(file: BookFile): Long

    suspend fun deleteFile(file: BookFile)
}