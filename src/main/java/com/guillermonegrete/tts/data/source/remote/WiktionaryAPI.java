package com.guillermonegrete.tts.data.source.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WiktionaryAPI {

    @GET("api.php?action=query&prop=extracts&format=json&explaintext=&redirects=1")
    Call<WiktionaryResponse> getDefinition(@Query("titles") String word);
}
