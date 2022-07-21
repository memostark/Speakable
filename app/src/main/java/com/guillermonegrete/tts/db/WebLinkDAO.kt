package com.guillermonegrete.tts.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebLinkDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(link: WebLink): Long

    @Update
    fun update(link: WebLink)

    @Delete
    suspend fun delete(link: WebLink)

    @Query("SELECT * FROM web_link ORDER BY lastRead DESC")
    fun getRecentLinks(): Flow<List<WebLink>>

    @Query("SELECT * FROM web_link WHERE url = :url")
    suspend fun getLink(url: String): WebLink?

    @Transaction
    fun upsert(file: WebLink) {
        val id = insert(file)
        if (id == -1L) {
            update(file)
        }
    }
}