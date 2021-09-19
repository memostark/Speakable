package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.data.Translation
import java.lang.Exception
import java.util.LinkedHashMap

class FakeTranslatorSource: TranslationSource {
    var translationsData: LinkedHashMap<String, Translation> = LinkedHashMap()

    override fun getTranslation(
        text: String,
        languageFrom: String,
        languageTo: String
    ): Translation {
        return translationsData[text] ?: throw Exception("Not found")
    }

    @VisibleForTesting
    fun addTranslation(vararg translations: Translation) {
        for (translation in translations) {
            translationsData[translation.sentences.first().orig] = translation
        }
    }
}