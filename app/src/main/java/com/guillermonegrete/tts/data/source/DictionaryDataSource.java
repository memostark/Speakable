package com.guillermonegrete.tts.data.source;

import androidx.annotation.NonNull;

import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;

import java.util.List;

public interface DictionaryDataSource {
    interface GetDefinitionCallback{

        void onDefinitionLoaded(@NonNull List<WikiItem> definitions);

        void onDataNotAvailable();
    }

    void getDefinition(@NonNull String word, @NonNull GetDefinitionCallback callback);
}
