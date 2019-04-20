package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.db.Words;

public interface WordDataSource {

    interface GetWordCallback{

        void onWordLoaded(Words word);

        void onDataNotAvailable();
    }

    void getWordLanguageInfo(String wordText, String language, GetWordCallback callback);
}
