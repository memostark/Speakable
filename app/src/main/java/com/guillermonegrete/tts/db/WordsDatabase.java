package com.guillermonegrete.tts.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Words.class}, version = 2)
public abstract class WordsDatabase extends RoomDatabase {
    public abstract WordsDAO wordsDAO();
}
