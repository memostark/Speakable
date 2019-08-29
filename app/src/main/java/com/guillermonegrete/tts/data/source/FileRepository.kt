package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile

interface FileRepository{
    suspend fun getRecentFiles(): List<BookFile>

    suspend fun saveFile(file: BookFile)
}