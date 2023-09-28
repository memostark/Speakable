package com.guillermonegrete.tts.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO

@Database(
    version = 10,
    entities = [BookFile::class, WebLink::class, Note::class],
    autoMigrations = [
        AutoMigration (from = 4, to = 5),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 9, to = 10),
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_8_9)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN percentageDone INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN folderPath TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN last_character INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration for adding a foreign key constraint to notes for the web link relationship
                // Because SQLite doesn't support altering a table to add a foreign key, it's necessary to create another table with the constraint
                // and then transfer the data, delete the old table and rename the new one

                // Create a new translation table
                database.execSQL("CREATE TABLE IF NOT EXISTS notes_new (" +
                        "`text` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`length` INTEGER NOT NULL, " +
                        "`color` TEXT NOT NULL, " +
                        "`file_id` INTEGER NOT NULL, " +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "FOREIGN KEY(`file_id`) REFERENCES `web_link`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                // Copy the data
                database.execSQL("INSERT INTO `notes_new` (text, position, length, color, file_id, id) " +
                        "SELECT text, position, length, color, file_id, id " +
                        "FROM notes")
                // Remove old table
                database.execSQL("DROP TABLE notes")
                // Change name of table to correct one
                database.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }
    }
}