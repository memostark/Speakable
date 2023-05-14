package com.guillermonegrete.tts.db

import androidx.room.*
import com.guillermonegrete.tts.webreader.db.LinkWithNotes
import kotlinx.coroutines.flow.Flow

@Dao
interface WebLinkDAO {

    @Update
    suspend fun update(link: WebLink)

    @Upsert
    suspend fun upsert(link: WebLink): Long

    @Delete
    suspend fun delete(link: WebLink)

    @Query("SELECT * FROM web_link ORDER BY lastRead DESC")
    fun getRecentLinks(): Flow<List<WebLink>>

    @Query("SELECT * FROM web_link WHERE url = :url")
    suspend fun getLink(url: String): WebLink?

    @Transaction
    @Query("SELECT * FROM web_link WHERE url = :url")
    suspend fun getLinkWithNotes(url: String): LinkWithNotes?
}
