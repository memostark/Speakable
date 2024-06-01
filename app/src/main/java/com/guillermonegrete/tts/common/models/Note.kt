package com.guillermonegrete.tts.common.models

import androidx.annotation.ColorInt

data class EditNote(
    val text: String,
    val noteText: String,
    val span: Span,
    @ColorInt val color: Int,
    val noteSaved: Boolean,
    val id: Long
)

data class NoteItem(
    val text: String,
    val span: Span,
    @ColorInt val color: Int,
    val id: Long
)
