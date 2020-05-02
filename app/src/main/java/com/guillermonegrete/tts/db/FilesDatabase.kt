package com.guillermonegrete.tts.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BookFile::class], version = 2)
@TypeConverters(Converters::class)
abstract class FilesDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDAO

    companion object{
        fun getDatabase(context: Context): FilesDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                FilesDatabase::class.java,
                "files.db")
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_files ADD COLUMN percentageDone INTEGER NOT NULL DEFAULT 0 ")
            }
        }
    }
}