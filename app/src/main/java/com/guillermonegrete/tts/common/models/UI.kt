package com.guillermonegrete.tts.common.models

import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.Words

data class WordUI(
    val word: String,
    val lang: String,
    val definition: String,
    val notes: String? = null
) {
    fun toWord() = Words(word, lang, definition)
}

fun Words.toUI() = WordUI(word, lang, definition, notes)

data class ExternalLinkUI(val siteName: String, val link: String, val language: String)

fun ExternalLink.toUI() = ExternalLinkUI(siteName, link, language)
