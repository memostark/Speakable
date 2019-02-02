package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.db.Words;

public interface WordRepositorySource {

    interface GetWordRepositoryCallback{

        void onLocalWordLoaded(Words word);

        void onRemoteWordLoaded(Words word);

        void onDataNotAvailable();
    }

    interface GetTranslationCallback{
        void onTranslationAndLanguage(Words word);

        void onDataNotAvailable();
    }

    void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback);

    void getLanguageAndTranslation(String text, GetTranslationCallback callback);
}
