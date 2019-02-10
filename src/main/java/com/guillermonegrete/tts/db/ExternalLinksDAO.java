package com.guillermonegrete.tts.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ExternalLinksDAO {

    @Query("SELECT * FROM links where language = :language")
    List<ExternalLink> findLinksByLanguage(String language);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ExternalLink link);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ExternalLink... links);

    @Query("DELETE FROM links")
    void deleteAll();

}
