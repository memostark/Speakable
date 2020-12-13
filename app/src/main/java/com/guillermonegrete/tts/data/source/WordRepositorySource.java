package com.guillermonegrete.tts.data.source;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.data.Result;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface WordRepositorySource {

    interface GetWordRepositoryCallback{

        void onLocalWordLoaded(Words word);

        void onLocalWordNotAvailable();

        void onRemoteWordLoaded(Words word);

        void onDataNotAvailable(Words emptyWord);
    }

    interface GetTranslationCallback{
        void onTranslationAndLanguage(Words word);

        void onDataNotAvailable();
    }

    List<Words> getWords();

    LiveData<List<Words>> getWordsStream();

    List<String> getLanguagesISO();

    void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback);

    void getWordLanguageInfo(@NonNull String wordText, @NonNull String languageFrom, @NonNull String languageTo, @NonNull GetWordRepositoryCallback callback);

    void getLanguageAndTranslation(@NonNull String text, @NonNull GetTranslationCallback callback);

    /**
     * Avoid usage of this function, only use when you can't use Kotlin Coroutines or RxJava
     * Use version that returns Result<Words>
     */
    void getLanguageAndTranslation(@NonNull String text, @NonNull String languageFrom, @NonNull String languageTo, @NonNull GetTranslationCallback callback);

    Result<Words> getLanguageAndTranslation(@NonNull String text, @NonNull String languageFrom, @NonNull String languageTo);

    void deleteWord(String word);

    void deleteWord(Words word);

    void delete(Words... words);

    void insert(Words... words);
}
