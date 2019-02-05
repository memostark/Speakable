package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLangHeader;

import java.util.List;

public class DictionaryRepository implements DictionaryDataSource{

    private final DictionaryDataSource mWiktionaryDataSource;

    public DictionaryRepository(DictionaryDataSource wiktionaryDataSource){
        mWiktionaryDataSource = wiktionaryDataSource;
    }



    @Override
    public void getDefinition(String word, GetDefinitionCallback callback) {
        mWiktionaryDataSource.getDefinition(word, new DictionaryDataSource.GetDefinitionCallback() {
            @Override
            public void onDefinitionLoaded(List<WikiItem> definitions) {

            }

            @Override
            public void onDataNotAvailable() {

            }
        });
    }
}
