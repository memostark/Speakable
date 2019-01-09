package com.guillermonegrete.tts.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordsDAO {

    @Query("SELECT * FROM words where wid = :id LIMIT 1")
    Words findWordById(int id);

    @Query("SELECT * FROM words where word = :word LIMIT 1")
    Words findWord(String word);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Words word);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Words... directors);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(Words words);

    @Query("DELETE FROM words where wid = :id")
    void deleteWordById(int id);

    @Query("DELETE FROM words where word = :word")
    void deleteWord(String word);

    @Query("DELETE FROM words")
    void deleteAll();

    @Query("SELECT * FROM words")
    LiveData<List<Words>> getAllWords();
}
