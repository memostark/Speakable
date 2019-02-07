package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WiktionarySource implements DictionaryDataSource {

    private static WiktionarySource INSTANCE;
    private static final String BASE_URL = "https://en.wiktionary.org/w/";

    private WiktionaryAPI wiktionaryAPI;


    public static WiktionarySource getInstance(){
        if(INSTANCE == null){
            INSTANCE = new WiktionarySource();
        }

        return INSTANCE;
    }

    private WiktionarySource(){
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        wiktionaryAPI = retrofit.create(WiktionaryAPI.class);
    }


    @Override
    public void getDefinition(String word, final GetDefinitionCallback callback) {
        // System.out.println("Retrieving remote dictionary data");

        wiktionaryAPI.getDefinition(word).enqueue(new Callback<WiktionaryResponse>() {
            @Override
            public void onResponse(Call<WiktionaryResponse> call, Response<WiktionaryResponse> response) {

                if(response.isSuccessful() && response.body() != null){

                    WiktionaryResponse.PageInfo info = response.body().getQuery().getPageNumber().firstEntry().getValue();
                    if(info.getExtract() != null) {
                        List<WikiItem> items = WiktionaryParser.parse(info.getExtract());
                        callback.onDefinitionLoaded(items);
                    }else {
                        callback.onDataNotAvailable();
                    }
                }else {
                    callback.onDataNotAvailable();
                }
            }

            @Override
            public void onFailure(Call<WiktionaryResponse> call, Throwable t) {

            }
        });

    }

    public static class WiktionaryParser{

        public static List<WikiItem> parse(String text){
            List<String> LanguageSections = getLanguages(text);

            String[] separated;
            List<WikiItem> items = new ArrayList<>();

            for (int i=1; i<LanguageSections.size(); i++){
                separated = LanguageSections.get(i).split("\n=== ");
                String lang = separated[0].split(" ")[0];

                items.add(new WiktionaryLangHeader(lang));

                int j;
                //Log.i(TAG,"--------------"+lang+"---------------");
                for (j=1; j<separated.length;j++){
                    String[] subheaders = separated[j].split(" ===\n");
                    //Log.i(TAG,"----Subheader " + j +": "+subheaders[0]);
                    //Log.i(TAG,subheaders[1]);
                    String[] subsubheader = subheaders[1].split("\n==== ");
                    for(int k=0; k<subsubheader.length; k++){
                        items.add(new WiktionaryItem(subsubheader[k].replace("====\n",""), subheaders[0]));
                    }
                }
            }

            return items;
        }

        private static List<String> getLanguages(String extract){
            String[] separated = extract.split("\n== ");
            List<String> langs = new ArrayList<>();
            Collections.addAll(langs, separated);
            return langs;
        }
    }
}
