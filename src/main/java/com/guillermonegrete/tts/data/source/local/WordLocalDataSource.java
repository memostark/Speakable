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
        System.out.println("Retrieving local word data");
        Words retrieved_word = mWordDAO.findWord(wordText);
        if(retrieved_word == null){
            System.out.println("Local word not found");
            callback.onDataNotAvailable();
        }else {
            callback.onWordLoaded(retrieved_word);
        }
    }
}
