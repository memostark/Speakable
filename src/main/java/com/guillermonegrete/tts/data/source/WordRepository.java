package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.db.Words;


import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class WordRepository implements WordRepositorySource {

    private static WordRepository INSTANCE;

    private final WordDataSource mWordMSTranslatorSource;

    private final WordDataSource mWordLocalDataSource;

    private WordRepository(WordDataSource wordMSTranslatorSource,
                           WordDataSource wordLocalDataSource){
        mWordMSTranslatorSource = checkNotNull(wordMSTranslatorSource);
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
    public void getWordLanguageInfo(final String wordText, final GetWordRepositoryCallback callback) {
        System.out.println("On trying to get language");
        mWordLocalDataSource.getWordLanguageInfo(wordText, new WordDataSource.GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                System.out.println("Local word retrieved");
                callback.onLocalWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                System.out.println("Try to get remote data");
                callback.onLocalWordNotAvailable();
                getRemoteWord(wordText, callback);
            }
        });

    }

    @Override
    public void getLanguageAndTranslation(String text, final GetTranslationCallback callback) {
        mWordMSTranslatorSource.getWordLanguageInfo(text, new WordDataSource.GetWordCallback() {
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

    private void getRemoteWord(String wordText, final GetWordRepositoryCallback callback) {
        mWordMSTranslatorSource.getWordLanguageInfo(wordText, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                System.out.println("External source retrieved");
                callback.onRemoteWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }
}
