package com.guillermonegrete.tts.webreader.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.WebLink
import kotlinx.parcelize.Parcelize

/**
 * Represents the text (note) applied to the specified position in a text file.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            onDelete = CASCADE,
            entity = WebLink::class,
            parentColumns = ["id"],
            childColumns = ["link_id"]
        ),
        ForeignKey(
            onDelete = CASCADE,
            entity = BookFile::class,
            parentColumns = ["bookFileId"],
            childColumns = ["book_id"]
        )
    ]
)
@Parcelize
data class Note(
    val text: String,
    @ColumnInfo(defaultValue = "")
    val originalText: String,
    /**
     * The starting character index of the note span in the text file.
     */
    val position: Int,
    /**
     * The length of the note span.
     */
    val length: Int,
    /**
     * Associated color of the note represented in hexadecimal ARGB. Example: "#aafdc817"
     */
    var color: String,
    @ColumnInfo(name = "link_id")
    val linkId: Int? = null,
    @ColumnInfo(name = "book_id")
    val bookId: Int? = null,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
): Parcelable {

    /**
     * For a book note, gets the position with the chapter. The actual position is in the first 24 bits of a 32 bit int
     */
    fun getPosInChapter() = position and 0xFFFFFF

    companion object {
        fun emptyNote(id: Long) = Note("", "", 0, 0, "", 0, null, id)

        fun getBookPosition(position: Int) = BookPosition(position shr 24, position and 0xFFFFFF)
    }
}

val Note.span: Span
    get() = Span(position, position + length)

val Note.spanBook: Span
    get() {
        val pos = getPosInChapter()
        return Span(pos, pos + length)
    }

data class LinkWithNotes(
    @Embedded val webLink: WebLink,
    @Relation(
        parentColumn = "id",
        entityColumn = "link_id"
    )
    val notes: List<Note>
)

data class NoteUpdate (
    val id: Long,
    val text: String,
    val color: String
)

data class BookPosition(val chapter: Int, val charPos: Int)
