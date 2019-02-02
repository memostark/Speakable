package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLanguage;

import java.util.List;

public interface DictionaryDataSource {
    interface GetDefinitionCallback{

        void onDefinitionLoaded(List<WiktionaryLanguage> definitions);

        void onDataNotAvailable();
    }

    void getDefinition(String word, GetDefinitionCallback callback);
}
