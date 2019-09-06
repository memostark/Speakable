package com.guillermonegrete.tts.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [BookFile::class], version = 1)
@TypeConverters(Converters::class)
abstract class FilesDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDAO
}