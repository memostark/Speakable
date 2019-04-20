package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.db.Words;

public interface WordRepositorySource {

    interface GetWordRepositoryCallback{

        void onLocalWordLoaded(Words word);

        void onLocalWordNotAvailable();

        void onRemoteWordLoaded(Words word);

        void onDataNotAvailable();
    }

    interface GetTranslationCallback{
        void onTranslationAndLanguage(Words word);

        void onDataNotAvailable();
    }

    void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback);

    void getWordLanguageInfo(String wordText, String language, GetWordRepositoryCallback callback);

    void getLanguageAndTranslation(String text, GetTranslationCallback callback);

    void getLanguageAndTranslation(String text, String language, GetTranslationCallback callback);

    void deleteWord(String word);

    void deleteWord(Words word);
}
