package com.guillermonegrete.tts.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

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

    @Query("DELETE FROM words")
    void deleteAll();

    @Query("SELECT * FROM words")
    LiveData<List<Words>> getAllDirectors();
}
