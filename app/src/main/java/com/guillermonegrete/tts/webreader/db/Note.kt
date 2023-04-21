package com.guillermonegrete.tts.webreader.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.guillermonegrete.tts.db.WebLink

/**
 * Represents the text (note) applied to the specified position in a text file.
 */
@Entity(tableName = "notes")
data class Note(
    val text: String,
    /**
     * The starting character index of the note span in the text file.
     */
    val position: Int,
    /**
     * The length of the note span.
     */
    val length: Int,
    var color: String,
    val fileId: Int,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "note_id")
    var id: Long = 0
)

data class LinkWithNotes(
    @Embedded val webLink: WebLink,
    @Relation(
        parentColumn = "id",
        entityColumn = "note_id"
    )
    val notes: List<Note>
)
