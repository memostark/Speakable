package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.inject.Singleton;

@Singleton
public class MSTranslatorSource implements WordDataSource {

    private static MSTranslatorSource INSTANCE;
    private static final String BASE_URL = "https://api.cognitive.microsofttranslator.com/";
    private final String apiKey;

    private MicrosoftTranslatorAPI MSTranslatorAPI;

    public static MSTranslatorSource getInstance(String apiKey){
        if(INSTANCE == null){
          INSTANCE = new MSTranslatorSource(apiKey);
        }

        return INSTANCE;
    }

    public MSTranslatorSource(String apiKey){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        MSTranslatorAPI = retrofit.create(MicrosoftTranslatorAPI.class);
        this.apiKey = apiKey;

    }

    @Override
    public void getWordLanguageInfo(final String wordText, String languageFrom, String languageTo, final GetWordCallback callback) {
        // final JSONArray body = createRequestBody(wordText);

        RequestBody body = new RequestBody(wordText);
        List<RequestBody> bodyList = new ArrayList<>();
        bodyList.add(body);
        MSTranslatorAPI.getWord(bodyList, apiKey).enqueue(new Callback<List<MSTranslatorResponse>>() {
            @Override
            public void onResponse(Call<List<MSTranslatorResponse>> call, final retrofit2.Response<List<MSTranslatorResponse>> response) {
                System.out.print(response.isSuccessful());
                if(response.isSuccessful() && response.body() != null){
                    MSTranslatorResponse response_word = response.body().get(0);
                    callback.onWordLoaded(new Words(wordText,
                            response_word.getDetectedLanguage().getLanguage(),
                            response_word.getTranslations().get(0).getText()));
                }else
                    callback.onDataNotAvailable();
            }

            @Override
            public void onFailure(Call<List<MSTranslatorResponse>> call, final Throwable t) {
                System.out.println("On failure:");
                System.out.println(t.getMessage());
                callback.onDataNotAvailable();
            }
        });


    }


    public static class RequestBody{
        private String Text;
        RequestBody(String Text){
            this.Text = Text;
        }

        @Override
        public String toString() {
            return String.format("{'Text': '%s'}", Text);
        }
    }
}
