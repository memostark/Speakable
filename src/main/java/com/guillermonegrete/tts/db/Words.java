package com.guillermonegrete.tts.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

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

    @ColumnInfo(name = "notes")
    public String notes;

    public Words(@NonNull String word, @NonNull String lang, @NonNull String definition){
        this.word = word;
        this.lang = lang;
        this.definition = definition;

    }
}
