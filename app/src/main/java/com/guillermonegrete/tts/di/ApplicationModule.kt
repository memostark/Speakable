package com.guillermonegrete.tts.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.data.source.DictionaryDataSource
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.data.source.local.AssetsExternalLinksSource
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.data.source.remote.WiktionarySource
import com.guillermonegrete.tts.db.WordsDatabase
import com.guillermonegrete.tts.threading.MainThreadImpl
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton
import dagger.Binds



@Module(includes = [ApplicationModuleBinds::class])
object ApplicationModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideAppContext(app: Application): Context = app.applicationContext

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

    @JvmStatic
    @Singleton
    @Provides
    fun provideExternalLinksSource(app: Application): ExternalLinksDataSource = AssetsExternalLinksSource(app)

    @JvmStatic
    @Singleton
    @Provides
    fun provideWiktionarySource(): DictionaryDataSource = WiktionarySource()
}

@Module
abstract class ApplicationModuleBinds {

    @Binds
    abstract fun bindExecutor(executor: ThreadExecutor): Executor

    @Binds
    abstract fun bindThread(executor: MainThreadImpl): MainThread
}