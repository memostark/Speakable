package com.guillermonegrete.tts.data.source.remote

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.*
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.TranslationSource
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePublicSource @Inject constructor(): WordDataSource, TranslationSource {

    private var googlePublicAPI: GooglePublicAPI
    private val gson: Gson = GsonBuilder()
            .setLenient()
            .create()

    init {
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        googlePublicAPI = retrofit.create(GooglePublicAPI::class.java)
    }

    /**
     *  These three functions should not be used for this implementation
     */
    override fun getWords(): MutableList<Words> = mutableListOf()

    override fun getLanguagesISO(): MutableList<String> = mutableListOf()

    override fun getWordsStream(): LiveData<MutableList<Words>> = MutableLiveData()

    override fun insertWords(vararg words: Words?) {}

    override fun deleteWords(vararg words: Words?) {}

    override fun getWordLanguageInfo(
        wordText: String?,
        languageFrom: String?,
        languageTo: String?,
        callback: WordDataSource.GetWordCallback?
    ) {
        if (wordText != null && languageTo!= null && languageFrom != null) {

            val rawLanguageFrom = if(languageFrom == "he") "iw" else languageFrom

            googlePublicAPI.getWord(wordText, rawLanguageFrom, languageTo).enqueue(object : Callback<GoogleTranslateResponse>{
                override fun onFailure(call: Call<GoogleTranslateResponse>, t: Throwable) {
                    callback?.onDataNotAvailable()
                }

                override fun onResponse(call: Call<GoogleTranslateResponse>, response: Response<GoogleTranslateResponse>) {

                    val body = response.body()
                    if(response.isSuccessful && body != null){
                        val parsedWord = processText(body, wordText)
                        callback?.onWordLoaded(parsedWord)
                    }else{
                        callback?.onDataNotAvailable()
                    }
                }

            })
        }
    }

    override fun getWordLanguageInfo(
        wordText: String,
        languageFrom: String,
        languageTo: String
    ): Words {
        val rawLanguageFrom = if(languageFrom == "he") "iw" else languageFrom

        val response = googlePublicAPI.getWord(wordText, rawLanguageFrom, languageTo).execute()
        val responseBody = response.body()

        if(!response.isSuccessful || responseBody == null) throw HttpException(response)

        return processText(responseBody, wordText)
    }

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

    private fun toTranslation(response: GoogleTranslateResponse)
            = Translation(response.sentences.map { Segment(it.trans, it.orig) }, response.src)

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
    }
}
