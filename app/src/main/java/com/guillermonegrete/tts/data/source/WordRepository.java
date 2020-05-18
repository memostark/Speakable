package com.guillermonegrete.tts.data.source;

import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.di.ApplicationModule;


import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

@Singleton
public class WordRepository implements WordRepositorySource {

    private final WordDataSource remoteTranslatorSource;

    private final WordDataSource mWordLocalDataSource;

    private ConcurrentMap<String, Words> cachedWords;


    @Inject
    public WordRepository(@ApplicationModule.RemoteTranslationDataSource WordDataSource remoteTranslatorSource,
                          @ApplicationModule.WordsLocalDataSource WordDataSource wordLocalDataSource){
        this.remoteTranslatorSource = checkNotNull(remoteTranslatorSource);
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);

        cachedWords = new ConcurrentHashMap<>();
    }

    @Override
    public List<Words> getWords() {
        return mWordLocalDataSource.getWords();
    }

    @Override
    public LiveData<List<Words>> getWordsStream() {
        return mWordLocalDataSource.getWordsStream();
    }

    @Override
    public List<String> getLanguagesISO() {
        return mWordLocalDataSource.getLanguagesISO();
    }

    @Override
    public void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback) {
        getWordLanguageInfo(wordText, "auto", "en", callback);
    }

    @Override
    public void getWordLanguageInfo(final String wordText, final String languageFrom, final String languageTo, final GetWordRepositoryCallback callback) {
        mWordLocalDataSource.getWordLanguageInfo(wordText, languageFrom, languageTo, new WordDataSource.GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                callback.onLocalWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onLocalWordNotAvailable();
                getRemoteWord(wordText, languageFrom, languageTo, callback);
            }
        });

    }

    @Override
    public void getLanguageAndTranslation(String text, GetTranslationCallback callback) {
        getLanguageAndTranslation(text, "auto", "en", callback);
    }

    @Override
    public void getLanguageAndTranslation(String text, String languageFrom, String languageTo, final GetTranslationCallback callback) {
        remoteTranslatorSource.getWordLanguageInfo(text, languageFrom , languageTo, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                callback.onTranslationAndLanguage(word);
                cachedWords.put(text, word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });

    }

    @Override
    public void deleteWord(String word) {
        WordLocalDataSource wordSource = (WordLocalDataSource) mWordLocalDataSource;
        wordSource.deleteWord(word);
    }

    @Override
    public void deleteWord(Words word) {
        WordLocalDataSource wordSource = (WordLocalDataSource) mWordLocalDataSource;
        wordSource.deleteWord(word);
    }

    @Override
    public void delete(Words... words) {
        mWordLocalDataSource.deleteWords(words);
    }

    @Override
    public void insert(Words... words) {
        mWordLocalDataSource.insertWords(words);
    }

    private void getRemoteWord(String wordText, String languageFrom, String languageTo, final GetWordRepositoryCallback callback) {

        Words cacheWord = cachedWords.get(wordText);
        if(cachedWords.get(wordText) != null){
            callback.onRemoteWordLoaded(cacheWord);
            return;
        }

        remoteTranslatorSource.getWordLanguageInfo(wordText, languageFrom, languageTo, new WordDataSource.GetWordCallback() {

            @Override
            public void onWordLoaded(Words word) {
                callback.onRemoteWordLoaded(word);
                cachedWords.put(wordText, word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable(new Words(wordText, "un", "un"));
            }
        });
    }
}
