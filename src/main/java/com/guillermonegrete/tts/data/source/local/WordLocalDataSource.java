package com.guillermonegrete.tts.data.source.local;

import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;

public class WordLocalDataSource implements WordDataSource {

    private static volatile WordLocalDataSource INSTANCE;

    private WordsDAO mWordDAO;

    private WordLocalDataSource(WordsDAO wordsDAO){
        mWordDAO = wordsDAO;
    }

    public static WordLocalDataSource getInstance(WordsDAO wordsDAO){
        if(INSTANCE == null){
            synchronized (WordLocalDataSource.class){
                if (INSTANCE == null)
                    INSTANCE = new WordLocalDataSource(wordsDAO);
            }
        }
        return INSTANCE;

    }

    @Override
    public void getWordLanguageInfo(String wordText, GetWordCallback callback) {
        System.out.print("Retrieving local word data");
        Words dummyWord = new Words("Dummy", "EN", "Dummy local translation");
        callback.onWordLoaded(dummyWord);

    }
}
