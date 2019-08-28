package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile

interface FileRepository{
    fun getRecentFiles(): List<BookFile>

    fun saveFile(file: BookFile)
}