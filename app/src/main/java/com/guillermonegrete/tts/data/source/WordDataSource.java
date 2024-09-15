package com.guillermonegrete.tts.data.source;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface WordDataSource {

    interface GetWordCallback{

        void onWordLoaded(Words word);

        void onDataNotAvailable();
    }

    interface GetWordsCallback{

        void onWordsLoaded(@NonNull List<Words> words);

        void onDataNotAvailable(@NonNull Exception exception);
    }

    List<Words> getWords();

    LiveData<List<Words>> getWordsStream();

    void findWords(final List<String> words, GetWordsCallback callback);

    List<String> getLanguagesISO();

    int insertWord(@NonNull Words word);

    void insertWords(Words... words);

    long upsert(@NonNull Words word);

    void deleteWords(Words... words);

    void getWordLanguageInfo(String wordText, String languageFrom, String languageTo, GetWordCallback callback);

    LiveData<Words> loadWord(String word, String language);

    int update(@NonNull Words word);
}
