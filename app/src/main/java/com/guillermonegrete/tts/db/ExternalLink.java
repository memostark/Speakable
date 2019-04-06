package com.guillermonegrete.tts.db;

import android.os.Parcel;
import android.os.Parcelable;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "links")
public class ExternalLink implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(siteName);
        parcel.writeString(link);
        parcel.writeString(language);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public ExternalLink createFromParcel(Parcel parcel) {
            return new ExternalLink(parcel);
        }

        @Override
        public ExternalLink[] newArray(int i) {
            return new ExternalLink[0];
        }
    };

    public ExternalLink(Parcel in){
        this.id = in.readInt();
        this.siteName = in.readString();
        this.link = in.readString();
        this.language = in.readString();
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                ", siteName='" + siteName + '\'' +
                ", link='" + link + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
