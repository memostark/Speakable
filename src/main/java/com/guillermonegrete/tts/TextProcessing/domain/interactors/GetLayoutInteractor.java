package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.TextProcessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLanguage;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface GetLayoutInteractor {
    interface Callback{
        void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType);

        void onDictionaryLayoutDetermined(List<WiktionaryLanguage> items);
    }
}
