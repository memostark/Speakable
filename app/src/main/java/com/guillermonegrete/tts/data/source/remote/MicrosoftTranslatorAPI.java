package com.guillermonegrete.tts.data.source.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface MicrosoftTranslatorAPI  {

    @Headers("Content-Type: application/json")
    @POST("translate?api-version=3.0&to=en")
    Call<List<MSTranslatorResponse>> getWord(@Body List<MSTranslatorSource.RequestBody> body, @Header("Ocp-Apim-Subscription-Key") String apiKey);
}
