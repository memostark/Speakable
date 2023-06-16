package com.guillermonegrete.tts.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordsDAO {

    @Query("SELECT * FROM words where word = :word LIMIT 1")
    Words findWord(String word);

    @Query("SELECT * FROM words WHERE word = :word AND lang = :language")
    LiveData<Words> loadWord(String word, String language);

    @Query("SELECT * FROM words WHERE wid = :id")
    LiveData<Words> loadWordById(int id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Words word);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Words... words);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int update(Words words);

    @Query("DELETE FROM words where word = :word")
    void deleteWord(String word);

    @Delete
    void deleteWords(Words... words);

    @Query("SELECT * FROM words")
    List<Words> getAllWords();

    @Query("SELECT * FROM words")
    LiveData<List<Words>> getAllWordsLive();

    @Query("SELECT DISTINCT LOWER(lang) FROM words")
    LiveData<List<String>> getLanguagesISOLiveData();

    @Query("SELECT DISTINCT LOWER(lang) FROM words")
    List<String> getLanguagesISO();
}
