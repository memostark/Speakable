package com.guillermonegrete.tts.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO

@Database(
    version = 15,
    entities = [BookFile::class, WebLink::class, Note::class],
    autoMigrations = [
        AutoMigration (from = 4, to = 5),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 9, to = 10),
        AutoMigration (from = 10, to = 11),
        AutoMigration (from = 11, to = 12, spec = FilesDatabase.RenameFileIdColumnMigration::class),
        AutoMigration (12, 13),
        AutoMigration (13, 14)
    ]

)
@TypeConverters(Converters::class)
abstract class FilesDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDAO

    abstract fun linkDao(): WebLinkDAO

    abstract fun noteDao(): NoteDAO

    companion object{
        fun getDatabase(context: Context): FilesDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                FilesDatabase::class.java,
                "files.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_8_9, MIGRATION_14_15)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE book_files ADD COLUMN percentageDone INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE book_files ADD COLUMN folderPath TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE book_files ADD COLUMN last_character INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration for adding a foreign key constraint to notes for the web link relationship
                // Because SQLite doesn't support altering a table to add a foreign key, it's necessary to create another table with the constraint
                // and then transfer the data, delete the old table and rename the new one

                // Create a new translation table
                db.execSQL("CREATE TABLE IF NOT EXISTS notes_new (" +
                        "`text` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`length` INTEGER NOT NULL, " +
                        "`color` TEXT NOT NULL, " +
                        "`file_id` INTEGER NOT NULL, " +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "FOREIGN KEY(`file_id`) REFERENCES `web_link`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                // Copy the data
                db.execSQL("INSERT INTO `notes_new` (text, position, length, color, file_id, id) " +
                        "SELECT text, position, length, color, file_id, id " +
                        "FROM notes")
                // Remove old table
                db.execSQL("DROP TABLE notes")
                // Change name of table to correct one
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This migration removes the duplicates in the uri column (persists the entry with the most notes) of book_files and adds a unique constraint for said column
                db.execSQL("""
                    DELETE FROM book_files
                    WHERE rowid NOT IN (
                        WITH BooksByNoteCount AS (
                            SELECT bookFileId, uri FROM book_files
                            LEFT JOIN notes ON notes.book_id = bookFileId
                            GROUP BY bookFileId
                            ORDER BY COUNT(notes.book_id) DESC
                        )
                        SELECT bookFileId FROM BooksByNoteCount
                        GROUP BY uri -- this removes duplicated uris and keeps the first id found, in this case the one with the most notes
                        
                        -- Alternative simpler query, picks the first created entry with the uri
                        -- SELECT MIN(rowid)
                        -- FROM book_files
                        -- GROUP BY uri 
                    );
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_book_files_uri_unique ON book_files(uri);
                """.trimIndent())
            }
        }
    }

    @RenameColumn(tableName = "notes", fromColumnName = "file_id", toColumnName = "link_id")
    class RenameFileIdColumnMigration : AutoMigrationSpec
}