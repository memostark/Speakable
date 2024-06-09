package com.guillermonegrete.tts.data.source.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WiktionaryAPI {

    @GET("api.php?action=parse&prop=text&format=json&redirects=1&formatversion=2")
    Call<WiktionaryResponse> getDefinition(@Query("page") String word);
}
