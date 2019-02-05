package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;

import java.util.List;

public interface DictionaryDataSource {
    interface GetDefinitionCallback{

        void onDefinitionLoaded(List<WikiItem> definitions);

        void onDataNotAvailable();
    }

    void getDefinition(String word, GetDefinitionCallback callback);
}
