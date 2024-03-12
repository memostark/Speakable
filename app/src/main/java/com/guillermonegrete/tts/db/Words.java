package com.guillermonegrete.tts.db;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Objects;

@Entity(tableName = "words")
public class Words implements Parcelable {
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

    @Nullable
    @ColumnInfo(name = "notes")
    public String notes;

    public Words(@NonNull String word, @NonNull String lang, @NonNull String definition){
        this.word = word;
        this.lang = lang;
        this.definition = definition;

    }


    public void setWord(@NonNull String word) {
        this.word = word;
    }

    public void setLang(@NonNull String lang) {
        this.lang = lang;
    }

    public void setDefinition(@NonNull String definition) {
        this.definition = definition;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    @NonNull
    public String getWord() {return word;}
    @NonNull
    public String getLang() { return lang; }
    @NonNull
    public String getDefinition() { return definition; }

    protected Words(Parcel in) {
        id = in.readInt();
        word = Objects.requireNonNull(in.readString());
        lang = Objects.requireNonNull(in.readString());
        definition = Objects.requireNonNull(in.readString());
        notes = in.readString();
    }

    public static final Creator<Words> CREATOR = new Creator<Words>() {
        @Override
        public Words createFromParcel(Parcel in) {
            return new Words(in);
        }

        @Override
        public Words[] newArray(int size) {
            return new Words[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(word);
        dest.writeString(lang);
        dest.writeString(definition);
        dest.writeString(notes);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Words(word=%s,lang=%s,definition=%s, notes=%s, id=%s)", word, lang, definition, notes, id);
    }
}
