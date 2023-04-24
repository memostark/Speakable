package com.guillermonegrete.tts.webreader.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDAO {

    @Insert
    suspend fun insert(note: Note)

    @Query("SELECT * FROM notes WHERE file_id = :fileId")
    suspend fun getNotes(fileId: Int): List<Note>

}
