package com.guillermonegrete.tts.db

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.guillermonegrete.tts.importtext.ImportedFileType
import java.util.*

/**
 * Value "und" for parameter language means language unknown
 */
data class BookFile(
    val uri: String,
    val title: String,
    val fileType: ImportedFileType,
    val language: String = "und",
    val page: Int = 0,
    val chapter: Int = 0,
    val lastRead: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "bookFileId") var id: Int = 0
)