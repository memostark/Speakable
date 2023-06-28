package com.guillermonegrete.tts.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface FileDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(file: BookFile): Long

    @Delete
    fun delete(file: BookFile)

    @Update
    fun update(file: BookFile)

    @Query("SELECT * FROM book_files ORDER BY lastRead DESC")
    fun getRecentFiles(): Flow<List<BookFile>>

    @Query("SELECT * FROM book_files")
    fun getFiles(): List<BookFile>

    @Query("SELECT * FROM book_files WHERE bookFileId = :id")
    fun getFile(id: Int): BookFile?

    @Query("SELECT * FROM book_files WHERE uri = :uri")
    fun getFile(uri: String): BookFile?

    @Upsert
    suspend fun upsert(file: BookFile): Long
}
