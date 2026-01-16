package com.guillermonegrete.tts.data.source.remote;

import androidx.annotation.NonNull;

import com.guillermonegrete.tts.common.UserAgentInterceptor;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class WiktionarySource implements DictionaryDataSource {

    private static final String BASE_URL = "https://en.wiktionary.org/w/";

    private final WiktionaryAPI wiktionaryAPI;

    public WiktionarySource(OkHttpClient client, Moshi moshi){
        client = client.newBuilder()
                .addInterceptor(new UserAgentInterceptor())
                .build();
        var retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build();

        wiktionaryAPI = retrofit.create(WiktionaryAPI.class);
    }


    @Override
    public void getDefinition(String word, final GetDefinitionCallback callback) {

        wiktionaryAPI.getDefinition(word).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WiktionaryResponse> call, @NonNull Response<WiktionaryResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    var pageEntry = response.body().getQuery().getPageNumber().entrySet().iterator().next();
                    if (pageEntry == null) {
                        callback.onDataNotAvailable();
                        return;
                    }

                    var info = pageEntry.getValue();
                    if (info.getExtract() != null) {
                        List<WikiItem> items = WiktionaryParser.parse(info.getExtract());
                        callback.onDefinitionLoaded(items);
                    } else {
                        callback.onDataNotAvailable();
                    }
                } else {
                    callback.onDataNotAvailable();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WiktionaryResponse> call, @NonNull Throwable t) {
                callback.onDataNotAvailable();
            }
        });

    }

    public static class WiktionaryParser {

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

                        // We remove undesirable equals
                        String firstFilter = subHeaderContent.replace("=====", "");
                        String itemBodyText = firstFilter.replace("====", "");

                        items.add(new WiktionaryItem(itemBodyText, subHeader));
                    } else { // Because some headers don't have text body
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
