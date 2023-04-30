package com.guillermonegrete.tts.webreader.model

import com.guillermonegrete.tts.webreader.db.Note

sealed class ModifiedNote {
    data class Update(val note: Note): ModifiedNote()
    data class Delete(val noteId: Long): ModifiedNote()
}
