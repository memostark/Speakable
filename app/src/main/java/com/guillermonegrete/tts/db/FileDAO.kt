package com.guillermonegrete.tts.db

import androidx.room.*
import androidx.room.Transaction



@Dao
abstract class FileDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(file: BookFile): Long

    @Update
    abstract fun update(file: BookFile)

    @Query("SELECT * FROM book_files")
    abstract fun getRecentFiles(): List<BookFile>

    @Transaction
    open fun upsert(file: BookFile) {
        println("Trying to insert file: $file")
        val id = insert(file)
        if (id == -1L) {
            println("Updating file: $file")
            update(file)
        }
    }
}