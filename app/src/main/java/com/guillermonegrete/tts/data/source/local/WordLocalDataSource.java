package com.guillermonegrete.tts.data.source.local;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;

import java.util.List;

public class WordLocalDataSource implements WordDataSource {

    private final WordsDAO mWordDAO;

    public WordLocalDataSource(WordsDAO wordsDAO){
        mWordDAO = wordsDAO;
    }

    @Override
    public List<Words> getWords() {
        return mWordDAO.getAllWords();
    }

    @Override
    public LiveData<List<Words>> getWordsStream() {
        return mWordDAO.getAllWordsLive();
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
    public LiveData<Words> loadWord(String word, String language) {
        return mWordDAO.loadWord(word, language);
    }

    @Override
    public int update(@NonNull Words word) {
        return mWordDAO.update(word);
    }

    @Override
    public void insertWords(Words... words) {
        mWordDAO.insert(words);
    }

    @Override
    public void deleteWords(Words... words) {
        mWordDAO.deleteWords(words);
    }

    public void deleteWord(String word) {
        mWordDAO.deleteWord(word);

    }

    public void deleteWord(Words word) {
        // TODO implement method to delete word by Words object
    }
}
