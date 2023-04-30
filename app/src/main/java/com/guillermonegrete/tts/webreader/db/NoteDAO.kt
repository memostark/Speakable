package com.guillermonegrete.tts.webreader.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NoteDAO {

    @Upsert
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE file_id = :fileId")
    suspend fun getNotes(fileId: Int): List<Note>

}
