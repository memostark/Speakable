package com.guillermonegrete.tts.db

import androidx.room.*
import androidx.room.Transaction



@Dao
abstract class FileDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(file: BookFile): Long

    @Delete
    abstract fun delete(file: BookFile)

    @Update
    abstract fun update(file: BookFile)

    @Query("SELECT * FROM book_files ORDER BY lastRead DESC")
    abstract fun getRecentFiles(): List<BookFile>

    @Query("SELECT * FROM book_files")
    abstract fun getFiles(): List<BookFile>

    @Query("SELECT * FROM book_files WHERE bookFileId = :id")
    abstract fun getFile(id: Int): BookFile?

    @Query("SELECT * FROM book_files WHERE uri = :uri")
    abstract fun getFile(uri: String): BookFile?

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