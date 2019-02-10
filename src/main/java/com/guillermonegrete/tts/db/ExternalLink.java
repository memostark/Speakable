package com.guillermonegrete.tts.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "links")
public class ExternalLink {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "lid")
    public int id;

    @ColumnInfo(name = "site")
    @NonNull
    public String siteName;

    @ColumnInfo(name = "link")
    @NonNull
    public String link;

    @ColumnInfo(name = "language")
    @NonNull
    public String language;

    public ExternalLink(@NonNull String siteName, @NonNull String link, @NonNull String language){
        this.siteName = siteName;
        this.link = link;
        this.language = language;
    }
}
