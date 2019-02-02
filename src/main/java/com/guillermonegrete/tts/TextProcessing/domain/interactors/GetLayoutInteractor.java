package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.TextProcessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.db.Words;

public interface GetLayoutInteractor {
    interface Callback{
        void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType);
    }
}
