package com.guillermonegrete.tts.data.source;

import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface WordDataSource {

    interface GetWordCallback{

        void onWordLoaded(Words word);

        void onDataNotAvailable();
    }

    List<Words> getWords();

    LiveData<List<Words>> getWordsStream();

    List<String> getLanguagesISO();

    void insertWords(Words... words);

    void getWordLanguageInfo(String wordText, String languageFrom, String languageTo, GetWordCallback callback);
}
