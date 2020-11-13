package com.guillermonegrete.tts.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.data.source.*
import com.guillermonegrete.tts.data.source.local.AssetsExternalLinksSource
import com.guillermonegrete.tts.data.source.remote.WiktionarySource
import com.guillermonegrete.tts.textprocessing.ProcessTextContract
import com.guillermonegrete.tts.textprocessing.ProcessTextPresenter
import com.guillermonegrete.tts.threading.MainThreadImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

/**
 * WordRepositorySource binding to use in tests.
 *
 * Hilt will inject a [FakeWordRepository] instead of a [WordRepository].
 */
@Module
@InstallIn(ApplicationComponent::class)
object TestApplicationModule {

    @Singleton
    @Provides
    fun provideAppContext(app: Application): Context = app.applicationContext

    @Singleton
    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideExternalLinksSource(app: Application): ExternalLinksDataSource = AssetsExternalLinksSource(app)

    @Singleton
    @Provides
    fun provideWiktionarySource(): DictionaryDataSource = WiktionarySource()
}

@Module
@InstallIn(ApplicationComponent::class)
abstract class TestApplicationModuleBinds {

    @Singleton
    @Binds
    abstract fun bindWordRepo(repo: FakeWordRepository): WordRepositorySource

    @Binds
    abstract fun bindPresenter(presenter: ProcessTextPresenter): ProcessTextContract.Presenter

    @Binds
    abstract fun bindExecutor(executor: ThreadExecutor): Executor

    @Binds
    abstract fun bindThread(executor: MainThreadImpl): MainThread

}