package com.guillermonegrete.tts.data.source.remote

import androidx.annotation.VisibleForTesting
import com.google.gson.*
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

/*  Expected JSON response scheme:
    [
       [
          [
             "my friend",
             "amigo mio",
             null,
             null,
             1
          ]
       ],
       null,
       "es",
       null,
       null,
       null,
       1,
       null,
       [
          [
             "es"
          ],
          null,
          [
             1
          ],
          [
             "es"
          ]
       ]
    ]

    With auto detect:
    [[["disappointment of that poor","decepción de ese pobre",null,null,3]],null,"es",null,null,null,0.97732538,null,[["es"],null,[0.97732538],["es"]]]

    With language selected:
    [[["disappointment of that poor","decepción de ese pobre",null,null,3]],null,"es"]



    Because response JSON cannot be converted to POJO we have to parse it manually.
*/

class GooglePublicSource: WordDataSource {

    private var googlePublicAPI: GooglePublicAPI? = null
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

    override fun insertWords(vararg words: Words?) {}

    override fun getWordLanguageInfo(
        wordText: String?,
        languageFrom: String?,
        languageTo: String?,
        callback: WordDataSource.GetWordCallback?
    ) {
        if (wordText != null && languageTo!= null && languageFrom != null) {

            val rawLanguageFrom = if(languageFrom == "he") "iw" else languageFrom

            googlePublicAPI?.getWord(wordText, rawLanguageFrom, languageTo)?.enqueue(object : Callback<ResponseBody>{
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback?.onDataNotAvailable()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    val body = response.body()
                    if(response.isSuccessful && body != null){
                        val parsedWord = processText(body.string(), wordText)
                        callback?.onWordLoaded(parsedWord)
                    }else{
                        callback?.onDataNotAvailable()
                    }
                }

            })
        }
    }

    @VisibleForTesting
    fun processText(bodyText: String, wordText: String): Words{
        val jsonArray  = gson.fromJson(bodyText, JsonArray::class.java)
        val jsonSentences = jsonArray[0].asJsonArray
        val sentences = arrayListOf<String>()
        for(i in 0 until  jsonSentences.size()){
            sentences.add(jsonSentences[i].asJsonArray[0].asString)
        }

        val lastElement = jsonArray.last()
        val rawLanguage = if(lastElement is JsonArray) lastElement.asJsonArray[0].asJsonArray[0].asString else lastElement.asString
        val detectedLanguage = if(rawLanguage == "iw") "he" else rawLanguage

        val translation = sentences.joinToString(separator = "")
        return Words(wordText, detectedLanguage, translation)
    }

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
        private var INSTANCE : GooglePublicSource? = null

        fun getInstance() =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: GooglePublicSource()
            }
    }
}