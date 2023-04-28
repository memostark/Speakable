package com.guillermonegrete.tts.webreader.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NoteDAO {

    @Upsert
    suspend fun upsert(note: Note): Long

    @Query("SELECT * FROM notes WHERE file_id = :fileId")
    suspend fun getNotes(fileId: Int): List<Note>

}
