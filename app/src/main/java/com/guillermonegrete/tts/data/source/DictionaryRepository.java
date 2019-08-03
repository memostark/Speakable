package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DictionaryRepository implements DictionaryDataSource{

    private static DictionaryRepository INSTANCE;

    private final DictionaryDataSource mWiktionaryDataSource;

    @Inject
    public DictionaryRepository(DictionaryDataSource wiktionaryDataSource){
        mWiktionaryDataSource = wiktionaryDataSource;
    }

    public static DictionaryRepository getInstance(DictionaryDataSource wiktionaryDataSource){
        if(INSTANCE == null){
            INSTANCE = new DictionaryRepository(wiktionaryDataSource);
        }

        return INSTANCE;
    }



    @Override
    public void getDefinition(String word, final GetDefinitionCallback callback) {
        mWiktionaryDataSource.getDefinition(word, new DictionaryDataSource.GetDefinitionCallback() {
            @Override
            public void onDefinitionLoaded(List<WikiItem> definitions) {
                callback.onDefinitionLoaded(definitions);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }
}
