package com.guillermonegrete.tts.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Database(entities = [BookFile::class], version = 3)
@TypeConverters(Converters::class)
abstract class FilesDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDAO

    companion object{
        fun getDatabase(context: Context): FilesDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                FilesDatabase::class.java,
                "files.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN percentageDone INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN folderPath TEXT NOT NULL DEFAULT '${UUID.randomUUID()}'")
            }
        }
    }
}