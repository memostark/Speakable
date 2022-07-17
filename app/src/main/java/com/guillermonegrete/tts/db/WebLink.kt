package com.guillermonegrete.tts.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "web_link")
data class WebLink(
    val url: String,
    val language: String? = null,
    var lastRead: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)