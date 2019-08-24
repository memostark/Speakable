package com.guillermonegrete.tts.data.source.local;

import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;

import java.util.List;

public class WordLocalDataSource implements WordDataSource {

    private WordsDAO mWordDAO;

    public WordLocalDataSource(WordsDAO wordsDAO){
        mWordDAO = wordsDAO;
    }

    @Override
    public List<Words> getWords() {
        return mWordDAO.getAllWords();
    }

    @Override
    public List<String> getLanguagesISO() {
        return mWordDAO.getLanguagesISO();
    }

    @Override
    public void getWordLanguageInfo(String wordText, String languageFrom, String languageTo, GetWordCallback callback) {
        Words retrieved_word = mWordDAO.findWord(wordText);
        if(retrieved_word == null){
            callback.onDataNotAvailable();
        }else {
            callback.onWordLoaded(retrieved_word);
        }
    }

    @Override
    public void insertWords(Words... words) {
        mWordDAO.insert(words);
    }

    public void deleteWord(String word) {
        mWordDAO.deleteWord(word);

    }

    public void deleteWord(Words word) {
        // TODO implement method to delete word by Words object
    }
}
