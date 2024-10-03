package com.guillermonegrete.tts.db;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface WordsDAO {

    @Query("SELECT * FROM words where word = :word LIMIT 1")
    Words findWord(String word);

    @Query("SELECT * FROM words where word in (:words)")
    Flow<List<Words>> findWordsStream(final List<String> words);

    @Query("SELECT * FROM words where word in (:words)")
    List<Words> findWords(final List<String> words);

    // The second condition is ignored if lang is null, see explanation here: https://stackoverflow.com/a/41141640/10244759
    @Query("SELECT * FROM words WHERE word = :word AND lang = COALESCE(NULLIF(:language , ''), lang)")
    LiveData<Words> loadWord(String word, @Nullable String language);

    @Query("SELECT * FROM words WHERE wid = :id")
    LiveData<Words> loadWordById(int id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Words word);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Words... words);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int update(Words words);

    /**
     * @return -1 if the operation was an update, otherwise the id of the inserted row.
     */
    @Upsert
    long upsert(Words word);

    @Query("DELETE FROM words where word = :word")
    void deleteWord(String word);

    @Delete
    void deleteWords(Words... words);

    @Delete
    void deleteWord(Words word);

    @Query("SELECT * FROM words")
    List<Words> getAllWords();

    @Query("SELECT * FROM words")
    LiveData<List<Words>> getAllWordsLive();

    @Query("SELECT DISTINCT LOWER(lang) FROM words")
    LiveData<List<String>> getLanguagesISOLiveData();

    @Query("SELECT DISTINCT LOWER(lang) FROM words")
    List<String> getLanguagesISO();
}
