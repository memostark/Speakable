package com.guillermonegrete.tts.db

import android.net.Uri
import com.guillermonegrete.tts.importtext.ImportedFileType
import java.util.*

data class BookFile(
    val uri: Uri,
    val title: String,
    val lastRead: Calendar,
    val language: String,
    val fileType: ImportedFileType,
    val page: Int = 0,
    val chapter: Int = 0
)