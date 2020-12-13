package com.guillermonegrete.tts.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.guillermonegrete.tts.importtext.ImportedFileType
import java.util.*

/**
 * Stores all metadata associated to represent a book.
 * Value "und" for parameter language means language unknown
 */
@Entity(tableName = "book_files")
data class BookFile(
    val uri: String,
    val title: String,
    val fileType: ImportedFileType,
    val language: String = "und",
    var folderPath: String =  UUID.randomUUID().toString(),
    /**
     *  TODO page is not a good indicator of the current position because page size varies, replace with last character.
     *  This is problematic specially when rotating screen because the number of pages changes.
     */
    var page: Int = 0,
    var chapter: Int = 0,
    var percentageDone: Int = 0,
    var lastRead: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "bookFileId") var id: Int = 0
)