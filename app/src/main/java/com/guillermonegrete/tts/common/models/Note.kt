package com.guillermonegrete.tts.common.models

import android.graphics.Color
import androidx.annotation.ColorInt

import com.guillermonegrete.tts.utils.toHex
import com.guillermonegrete.tts.webreader.db.Note


data class EditNote(
    val text: String,
    val noteText: String,
    val span: Span,
    @ColorInt val color: Int,
    val noteSaved: Boolean,
    val id: Long
)

fun EditNote.toNote() = Note(noteText, text, span.start, span.end - span.start, color.toHex(), id = id)

fun Note.toEditNote() = EditNote(originalText, text, Span(position, position + length), if (color.isNotEmpty()) Color.parseColor(color) else 0, id != 0L, id)

data class NoteItem(
    val text: String,
    val span: Span,
    @ColorInt val color: Int,
    val id: Long
)
