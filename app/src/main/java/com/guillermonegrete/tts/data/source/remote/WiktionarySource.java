package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WiktionarySource implements DictionaryDataSource {

    private static final String BASE_URL = "https://en.wiktionary.org/w/";

    private WiktionaryAPI wiktionaryAPI;

    public WiktionarySource(){
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
            List<String> languageSections = getLanguages(text);
            List<WikiItem> items = new ArrayList<>();

            for (String languageSection: languageSections){
                String[] separated = languageSection.split("\n=== ");
                String lang = separated[0].split(" ")[0];

                items.add(new WiktionaryLangHeader(lang));

                List<String> langSubHeaders = new ArrayList<>(Arrays.asList(separated));
                langSubHeaders.remove(0);

                for (String langSubHeader: langSubHeaders){
                    String[] subHeaders = langSubHeader.split(" ===\n");
                    String subHeader = subHeaders[0];

                    if(subHeaders.length > 1) {
                        String subHeaderContent = subHeaders[1];
                        /*String[] subsubheader = subHeaders[1].split("\n==== ");
                        for (String subsub : subsubheader) {
                            items.add(new WiktionaryItem(subsub.replace("====\n", ""), subHeader));
                        }*/
                        items.add(new WiktionaryItem(subHeaderContent.replace("====", ""), subHeader));
                    } else {
                        items.add(new WiktionaryItem("", subHeader.replace("===", "")));
                    }
                }
            }

            return items;
        }

        public static List<String> getLanguages(String extract){
            String[] separated = extract.split("\n== ");
            List<String> langs = new ArrayList<>(Arrays.asList(separated));
            langs.remove(0);
            return langs;
        }
    }
}
