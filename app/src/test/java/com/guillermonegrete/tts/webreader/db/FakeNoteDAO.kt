package com.guillermonegrete.tts.webreader.db

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

    override suspend fun delete(note: Note) {
        notes.removeIf { it.id == note.id }
    }

    override suspend fun deleteByFileId(id: Int) {
        notes.removeIf { it.fileId == id }
    }

    override suspend fun getNotes(fileId: Int): List<Note> {
        return emptyList()
    }
}
