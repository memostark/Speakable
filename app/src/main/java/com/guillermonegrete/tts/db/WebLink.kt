package com.guillermonegrete.tts.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "web_link",
    indices = [Index(value = ["url"], unique = true)]
)
data class WebLink(
    val url: String,
    val title: String? = null,
    var language: String? = null,
    var lastRead: Calendar = Calendar.getInstance(),
    var uuid: UUID? = null,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)