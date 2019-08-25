package com.guillermonegrete.tts.db

import android.net.Uri

data class BookFile(
    val uri: Uri,
    val title: String,
    val lastRead: Int,
    val page: Int = 0,
    val chapter: Int = 0
)