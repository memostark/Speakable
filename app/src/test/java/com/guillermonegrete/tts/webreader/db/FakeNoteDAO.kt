package com.guillermonegrete.tts.webreader.db

import kotlinx.coroutines.flow.Flow

class FakeNoteDAO: NoteDAO {

    val notes = mutableListOf<Note>()

    override suspend fun upsert(note: Note): Long {
        return if (note in notes) {
            notes.removeIf { it.id == note.id }
            notes.add(note)
            -1
        } else {
            notes.add(note)
            note.id
        }
    }

    override suspend fun update(note: NoteUpdate): Int {
        TODO("Not yet implemented")
    }

    override suspend fun delete(note: Note) {
        notes.removeIf { it.id == note.id }
    }

    override suspend fun deleteByLinkId(id: Int) {
        notes.removeIf { it.linkId == id }
    }

    override suspend fun getNotes(linkId: Int): List<Note> {
        return emptyList()
    }

    override fun getLinkNotes(linkId: Int): Flow<List<Note>> {
        TODO("Not yet implemented")
    }

    override fun getFileNotes(fileId: Int): Flow<List<Note>> {
        TODO("Not yet implemented")
    }

    override suspend fun getFileNotes(bookId: Int, chapterIndex: Int): List<Note> {
        return emptyList()
    }
}
