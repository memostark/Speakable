package com.guillermonegrete.tts.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebLinkDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(file: WebLink): Long

    @Update
    fun update(file: WebLink)

    @Query("SELECT * FROM web_link ORDER BY lastRead DESC")
    fun getRecentLinks(): Flow<List<WebLink>>

    @Transaction
    fun upsert(file: WebLink) {
        val id = insert(file)
        if (id == -1L) {
            update(file)
        }
    }
}