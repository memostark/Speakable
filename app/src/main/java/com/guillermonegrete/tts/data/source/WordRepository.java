package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.di.ApplicationModule;


import javax.inject.Inject;

import java.util.List;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class WordRepository implements WordRepositorySource {

    private static WordRepository INSTANCE;

    private final WordDataSource remoteTranslatorSource;

    private final WordDataSource mWordLocalDataSource;


    @Inject
    public WordRepository(@ApplicationModule.RemoteTranslationDataSource WordDataSource remoteTranslatorSource,
                          @ApplicationModule.WordsLocalDataSource WordDataSource wordLocalDataSource){
        this.remoteTranslatorSource = checkNotNull(remoteTranslatorSource);
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);
    }

    public static WordRepository getInstance(WordDataSource wordMSTranslatorSource,
                           WordDataSource wordLocalDataSource){
        if(INSTANCE == null){
            INSTANCE = new WordRepository(wordMSTranslatorSource, wordLocalDataSource);
        }

        return INSTANCE;
    }

    @Override
    public List<Words> getWords() {
        return mWordLocalDataSource.getWords();
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
    public void insert(Words... words) {
        mWordLocalDataSource.insertWords(words);
    }

    private void getRemoteWord(String wordText, String languageFrom, String languageTo, final GetWordRepositoryCallback callback) {
        remoteTranslatorSource.getWordLanguageInfo(wordText, languageFrom, languageTo, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                callback.onRemoteWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }
}
