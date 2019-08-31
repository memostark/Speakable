package com.guillermonegrete.tts.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface FileDAO {

    @Query("SELECT * FROM book_files")
    fun getRecentFiles(): List<BookFile>
}