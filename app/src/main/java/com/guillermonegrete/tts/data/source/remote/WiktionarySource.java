package com.guillermonegrete.tts.data.source.remote;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WiktionarySource implements DictionaryDataSource {

    private static final String BASE_URL = "https://en.wiktionary.org/w/";

    private final WiktionaryAPI wiktionaryAPI;

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

        wiktionaryAPI.getDefinition(word).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WiktionaryResponse> call, @NonNull Response<WiktionaryResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    var parseAction = response.body().getParse();
                    if(parseAction != null) {
                        String htmlText = parseAction.getText();
                        var items = WiktionaryParser.parse(htmlText);
                        callback.onDefinitionLoaded(items);
                    } else {
                        // If the word doesn't exist then parse is null
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

        public static List<WikiItem> parse(String htmlText) {
            Document doc = Jsoup.parse(htmlText);
            // Remove table of contents at the start
            var toc = doc.getElementById("toc");
            if (toc != null) toc.remove();
            // Remove all the edit buttons/text
            var editSections = doc.getElementsByClass("mw-editsection");
            if (editSections != null) editSections.remove();
            CharSequence info = formatHtml(doc.outerHtml());
            return List.of(new WiktionaryItem(info, ""));
        }
    }

    /**
     * Format the raw xhtml text to get a more accurate length of the text.
     */
    private static Spanned formatHtml(CharSequence text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT);
        } else {
            //noinspection deprecation
            return Html.fromHtml(text.toString());
        }
    }
}
