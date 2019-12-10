package com.guillermonegrete.tts.data.source.remote

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.*
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

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

    // Latest response as of 04/Dec/2019
    [[["tiny","winzige",null,null,0]],null,"de",null,null,null,null,[]]

    Because response JSON cannot be converted to POJO we have to parse it manually.
*/
@Singleton
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

    override fun getWordsStream(): LiveData<MutableList<Words>> = MutableLiveData()

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
                        try {
                            val parsedWord = processText(body.string(), wordText)
                            callback?.onWordLoaded(parsedWord)
                        }catch (e: Exception){
                            callback?.onDataNotAvailable()
                        }

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

        // We extract all the posible translations from the first element
        // E.g. [["tiny","winzige",null,null,0], ["little","winzige",null,null,0]]
        val jsonSentences = jsonArray[0].asJsonArray
        val sentences = arrayListOf<String>()
        for(i in 0 until  jsonSentences.size()){
            sentences.add(jsonSentences[i].asJsonArray[0].asString)
        }

        val rawLanguage = jsonArray[2].asString
        val detectedLanguage = if(rawLanguage == "iw") "he" else rawLanguage

        val translation = sentences.joinToString(separator = "")
        return Words(wordText, detectedLanguage, translation)
    }

    companion object {
        const val BASE_URL = "https://translate.google.com/translate_a/"
    }
}