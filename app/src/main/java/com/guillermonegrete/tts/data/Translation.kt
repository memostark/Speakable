package com.guillermonegrete.tts.data

import com.guillermonegrete.tts.db.Words

data class Translation(
    val sentences: List<Segment>,
    /**
     * Original language of the translation (language from).
     */
    val src: String
){

    val originalText: String = sentences.joinToString("") { it.orig }

    val translatedText = sentences.joinToString(""){ it.trans }
}

data class Segment(val trans: String, val orig: String)

fun Translation.toWord() = Words(originalText, src, translatedText)

/**
 * Used as the key for the cache of remote translations.
 */
data class TranslationKey(val word: String, val languageFrom: String, val languageTo: String)
