package com.guillermonegrete.tts.data.source.remote

import com.google.gson.*
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    Because response JSON cannot be converted to POJO we have to parse it manually.
*/

class GooglePublicSource private constructor() : WordDataSource {

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


    override fun getWordLanguageInfo(wordText: String?, callback: WordDataSource.GetWordCallback?) {
        if (wordText != null) {
            googlePublicAPI?.getWord(wordText)?.enqueue(object : Callback<ResponseBody>{
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback?.onDataNotAvailable()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    val body = response.body()
                    if(response.isSuccessful && body != null){
                        val jsonArray  = gson.fromJson(body.string(), JsonArray::class.java)
                        val jsonSentences = jsonArray[0].asJsonArray
                        val sentences = arrayListOf<String>()
                        for(i in 0 until  jsonSentences.size()){
                            sentences.add(jsonSentences[i].asJsonArray[0].asString)
                        }

                        val rawLanguage = jsonArray.last().asJsonArray[0].asJsonArray[0].toString()
                        val language = if(rawLanguage == "\"iw\"") "he" else rawLanguage.substring(1, rawLanguage.length - 1)

                        val translation = sentences.joinToString(separator = "")
                        callback?.onWordLoaded(Words(wordText, language, translation))
                    }else{
                        callback?.onDataNotAvailable()
                    }
                }

            })
        }
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