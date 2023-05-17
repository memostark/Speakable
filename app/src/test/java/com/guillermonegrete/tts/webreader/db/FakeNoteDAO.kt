package com.guillermonegrete.tts.webreader.db

class FakeNoteDAO: NoteDAO {
    override suspend fun upsert(note: Note): Long {
        TODO("Not yet implemented")
    }

    override suspend fun delete(note: Note) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteByFileId(id: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun getNotes(fileId: Int): List<Note> {
        return emptyList()
    }
}
