package com.guillermonegrete.tts.data.source.remote

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePublicAPI {

    // Parameter unofficial reference: https://stackoverflow.com/questions/26714426/what-is-the-meaning-of-google-translate-query-params
    @GET("single?client=gtx&sl=auto&dt=t&oe=UTF-8&ie=UTF-8")
    fun getWord(@Query("q") text: String, @Query("tl") languageTo: String): Call<ResponseBody>
}