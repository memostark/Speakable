package com.guillermonegrete.tts.db;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
    @SerializedName("detectedLanguage.language")
    @Expose
    public String lang;

    @ColumnInfo(name = "definition")
    @NonNull
    @SerializedName("translations.text")
    @Expose
    public String definition;

    @ColumnInfo(name = "notes")
    public String notes;

    public Words(@NonNull String word, @NonNull String lang, @NonNull String definition){
        this.word = word;
        this.lang = lang;
        this.definition = definition;

    }

    public void setWord(String word){this.word = word;}
    public void setLang(String lang){this.lang = lang;}
    public void setDefinition(String definition){this.definition = definition;}

    public String getWord() {return word;}
    public String getLang() { return lang; }
    public String getDefinition() { return definition; }
}
