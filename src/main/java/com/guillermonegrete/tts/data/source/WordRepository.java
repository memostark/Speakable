package com.guillermonegrete.tts.data.source;


import com.guillermonegrete.tts.db.Words;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class WordRepository implements WordRepositorySource {

    private final WordDataSource mWordMSTranslatorSource;

    private final WordDataSource mWordLocalDataSource;

    private final DictionaryDataSource mWiktionaryDataSource;

    private WordRepository(WordDataSource wordMSTranslatorSource,
                           WordDataSource wordLocalDataSource,
                           DictionaryDataSource wiktionaryDataSource){
        mWordMSTranslatorSource = checkNotNull(wordMSTranslatorSource);
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);
        mWiktionaryDataSource = checkNotNull(wiktionaryDataSource);
    }

    @Override
    public void getWordLanguageInfo(final String wordText, final GetWordRepositoryCallback callback) {
        System.out.print("On trying to get language");
        mWordLocalDataSource.getWordLanguageInfo(wordText, new WordDataSource.GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                System.out.print("Local word retrieved");
                callback.onLocalWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                System.out.print("Try to get remote data");
                // implement microsoft translator and wiktionary request
                // Return both if wiktionary entry exits
                getRemoteWord(wordText, callback);
                getWiktionary(wordText, callback);
            }
        });

    }

    @Override
    public void getLanguageAndTranslation(String text, GetTranslationCallback callback) {
        mWordMSTranslatorSource.getWordLanguageInfo(text, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {

            }

            @Override
            public void onDataNotAvailable() {

            }
        });

    }

    private void getRemoteWord(String wordText, final GetWordRepositoryCallback callback) {
        mWordMSTranslatorSource.getWordLanguageInfo(wordText, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                System.out.print("External source retrieved");
                callback.onRemoteWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }

    private void getWiktionary(String wordText, GetWordRepositoryCallback callback) {

    }
}
