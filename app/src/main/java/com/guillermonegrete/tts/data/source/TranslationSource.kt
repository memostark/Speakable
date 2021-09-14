package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.data.Translation

/**
 * Source for the translation of a word or text (usually remote).
 * Unlike [WordDataSource], this is for a source that doesn't allow to save, update or delete data
 */
interface TranslationSource {
    fun getTranslation(text: String, languageFrom: String, languageTo: String): Translation
}