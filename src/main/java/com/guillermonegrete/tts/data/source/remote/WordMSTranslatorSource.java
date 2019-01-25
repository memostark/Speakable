package com.guillermonegrete.tts.data.source.remote;

import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;

public class WordMSTranslatorSource implements WordDataSource {

    private WordMSTranslatorSource(){

    }

    @Override
    public void getWordLanguageInfo(String wordText, GetWordCallback callback) {
        System.out.print("Retrieving remote word data");
        Words dummyWord = new Words("Dummy", "EN", "Dummy remote translation");
        callback.onWordLoaded(dummyWord);

    }
}
