package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.db.Words;


import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class WordRepository implements WordRepositorySource {

    private static WordRepository INSTANCE;

    private final WordDataSource remoteTranslatorSource;

    private final WordDataSource mWordLocalDataSource;


    private WordRepository(WordDataSource remoteTranslatorSource,
                           WordDataSource wordLocalDataSource){
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
    public void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback) {
        getWordLanguageInfo(wordText, "en", callback);
    }

    @Override
    public void getWordLanguageInfo(final String wordText, final String language, final GetWordRepositoryCallback callback) {
        mWordLocalDataSource.getWordLanguageInfo(wordText, language, new WordDataSource.GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                callback.onLocalWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onLocalWordNotAvailable();
                getRemoteWord(wordText, language, callback);
            }
        });

    }

    @Override
    public void getLanguageAndTranslation(String text, GetTranslationCallback callback) {
        getLanguageAndTranslation(text, "en", callback);
    }

    @Override
    public void getLanguageAndTranslation(String text, String language, final GetTranslationCallback callback) {
        remoteTranslatorSource.getWordLanguageInfo(text, language, new WordDataSource.GetWordCallback() {
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

    private void getRemoteWord(String wordText, String language, final GetWordRepositoryCallback callback) {
        remoteTranslatorSource.getWordLanguageInfo(wordText, language, new WordDataSource.GetWordCallback() {
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
