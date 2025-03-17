package com.guillermonegrete.tts.data.source.remote

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.TranslationSource
import com.guillermonegrete.tts.db.Words
import retrofit2.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePublicSource @Inject constructor(private val googlePublicAPI: GooglePublicAPI): TranslationSource {

    override fun getTranslation(
        text: String,
        languageFrom: String,
        languageTo: String
    ): Translation {
        val rawLanguageFrom = if(languageFrom == "he") "iw" else languageFrom

        val response = googlePublicAPI.getWord(text, rawLanguageFrom, languageTo).execute()
        val responseBody = response.body()

        if(!response.isSuccessful || responseBody == null) throw HttpException(response)

        return toTranslation(responseBody)
    }

    @VisibleForTesting
    fun processText(response: GoogleTranslateResponse, wordText: String): Words{
        return Words(wordText, response.src, response.sentences.joinToString(""){ it.trans })
    }

    /**
     * Converts to translation type from the domain.
     * The translate API removes the space between the sentences which leads to mismatches with the input text, so it has to be re added
     */
    private fun toTranslation(response: GoogleTranslateResponse): Translation{
        val segments = response.sentences.map { Segment(it.trans, it.orig) }
        val language = if(response.src == "iw") "he" else response.src
        return Translation(segments, language)
    }

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
    }
}
