package com.guillermonegrete.tts.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guillermonegrete.tts.webreader.db.Note

@Database(
    version = 8,
    entities = [BookFile::class, WebLink::class, Note::class],
    autoMigrations = [
        AutoMigration (from = 4, to = 5),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 7, to = 8),
    ]

)
@TypeConverters(Converters::class)
abstract class FilesDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDAO

    abstract fun linkDao(): WebLinkDAO

    companion object{
        fun getDatabase(context: Context): FilesDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                FilesDatabase::class.java,
                "files.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_2_3, MIGRATION_3_4)
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
    }
}