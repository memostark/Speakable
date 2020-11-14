package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.data.source.*
import dagger.Binds
import dagger.Module
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
abstract class TestApplicationModuleBinds {

    @Singleton
    @Binds
    abstract fun bindWordRepo(repo: FakeWordRepository): WordRepositorySource
}