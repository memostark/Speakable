package com.guillermonegrete.tts.data.source;

import com.guillermonegrete.tts.db.Words;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class WordRepository implements WordDataSource{

    private final WordDataSource mWordMSTranslatorSource;

    private final WordDataSource mWordLocalDataSource;

    private WordRepository(WordDataSource wordMSTranslatorSource,
                           WordDataSource wordLocalDataSource){
        mWordMSTranslatorSource = checkNotNull(wordMSTranslatorSource);
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);
    }

    @Override
    public void getWordLanguageInfo(String wordText, GetWordCallback callback) {
        System.out.print("On trying to get language");
        mWordLocalDataSource.getWordLanguageInfo(wordText, new GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                System.out.print("First trying to get local data");
            }

            @Override
            public void onDataNotAvailable() {
                System.out.print("Try to get remote data");
                // implement microsoft translator
            }
        });

    }
}
