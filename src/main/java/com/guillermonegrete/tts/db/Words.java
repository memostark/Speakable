package com.guillermonegrete.tts.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "words")
public class Words {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "wid")
    public int id;

    @ColumnInfo(name = "word")
    @NonNull
    public String word;
    @ColumnInfo(name = "lang")
    @NonNull
    public String lang;
    @ColumnInfo(name = "definition")
    @NonNull
    public String definition;

    public Words(@NonNull String word, @NonNull String lang, @NonNull String definition){
        this.word = word;
        this.lang = lang;
        this.definition = definition;

    }
}
