package com.guillermonegrete.tts.di

import android.content.Context
import androidx.room.Room
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.db.WordsDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
object ApplicationModule {

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class GooglePublicDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class WordsLocalDataSource

    @JvmStatic
    @Singleton
    @WordsLocalDataSource
    @Provides
    fun provideLocalSource(database: WordsDatabase): WordDataSource = WordLocalDataSource(database.wordsDAO())

    @JvmStatic
    @Singleton
    @Provides
    fun provideWordsDatabase(context: Context): WordsDatabase{
        return Room.databaseBuilder(
            context.applicationContext,
            WordsDatabase::class.java,
            "words.db"
        ).build()
    }

    @JvmStatic
    @Singleton
    @GooglePublicDataSource
    @Provides
    fun provideGooglePublicSource(): WordDataSource = GooglePublicSource()
}