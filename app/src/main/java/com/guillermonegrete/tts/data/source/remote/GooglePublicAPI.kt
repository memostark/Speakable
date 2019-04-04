package com.guillermonegrete.tts.data.source.remote

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePublicAPI {

    @GET("single?client=gtx&sl=auto&tl=en&dt=t")
    fun getWord(@Query("q") text: String): Call<ResponseBody>
}