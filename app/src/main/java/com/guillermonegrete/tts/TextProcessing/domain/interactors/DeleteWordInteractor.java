package com.guillermonegrete.tts.TextProcessing.domain.interactors;

public interface DeleteWordInteractor {
    interface Callback{
        void onWordDeleted();
    }
}
