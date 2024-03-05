package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.data.Translation;
import com.guillermonegrete.tts.textprocessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface GetLayoutInteractor {
    interface Callback{
        void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType);

        void onSentenceLayout(Translation translation);

        void onDictionaryLayoutDetermined(Words word, List<WikiItem> items);

        void onTranslationError(String message);
    }
}
