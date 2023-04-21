package com.guillermonegrete.tts.webreader.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface NoteDAO {

    @Insert
    suspend fun insert(note: Note)

}
