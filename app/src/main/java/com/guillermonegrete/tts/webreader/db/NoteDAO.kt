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

    @Query("DELETE FROM notes WHERE link_id = :id")
    suspend fun deleteByLinkId(id: Int)

    @Query("SELECT * FROM notes WHERE link_id = :linkId")
    suspend fun getNotes(linkId: Int): List<Note>

    // The chapter is encoded in the last 8 bits of the position (32 bit int) so we search notes between the chapter index and the index plus 1 (shifted left 24 represented by the constant 0x1000000)
    @Query("SELECT * FROM notes WHERE book_id = :bookId AND (position BETWEEN :chapterIndex AND (:chapterIndex + 0x1000000))")
    suspend fun getFileNotes(bookId: Int, chapterIndex: Int): List<Note>

}
