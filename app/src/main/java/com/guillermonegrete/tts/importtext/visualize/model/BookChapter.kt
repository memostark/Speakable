package com.guillermonegrete.tts.importtext.visualize.model

import com.guillermonegrete.tts.webreader.db.Note

data class BookChapter(val pages: List<CharSequence>, val notes: List<Note>)
