package com.guillermonegrete.tts.data.source.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePublicAPI {

    /**
     * Gets the translation of the given text to the indicated language, the source language can be specified or let the translator determine it.
     *
     * The parameters dt = t & dj = 1 are the most relevant for this use case, the first indicates to return a translation
     * and the second indicates to return a json with named objects and lists, which is easier to parse.
     * The rest of the parameters are necessary and should always be the same.
     *
     * Parameter unofficial reference: https://stackoverflow.com/questions/26714426/what-is-the-meaning-of-google-translate-query-params
     */
    @GET("single?client=gtx&dt=t&oe=UTF-8&ie=UTF-8&dj=1")
    fun getWord(@Query("q") text: String, @Query("sl") languageFrom: String, @Query("tl") languageTo: String): Call<GoogleTranslateResponse>
}
