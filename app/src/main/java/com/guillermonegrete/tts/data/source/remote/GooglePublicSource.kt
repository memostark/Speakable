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
        val sentences = response.sentences
        val segments = mutableListOf<Segment>()

        for(i in 0 until sentences.size - 1){
            val sentence = sentences[i]
            // If the original line ended with a new line the translate API doesn't change it
            // Then it's not necessary to add a white space
            val space = if(sentence.orig.endsWith("\n")) "" else " "
            val segment = Segment(sentence.trans, sentence.orig + space)
            segments.add(segment)
        }

        // The final line is unaffected by the api, so don't modify it.
        segments.add(Segment(sentences.last().trans, sentences.last().orig))

        val language = if(response.src == "iw") "he" else response.src
        return Translation(segments, language)
    }

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
    }
}
