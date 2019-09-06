package com.guillermonegrete.tts.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.guillermonegrete.tts.importtext.ImportedFileType
import java.util.*

/**
 * Value "und" for parameter language means language unknown
 */
@Entity(tableName = "book_files")
data class BookFile(
    val uri: String,
    val title: String,
    val fileType: ImportedFileType,
    val language: String = "und",
    var page: Int = 0,
    var chapter: Int = 0,
    var lastRead: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "bookFileId") var id: Int = 0
)