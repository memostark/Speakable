package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.customtts.FakeTTS
import com.guillermonegrete.tts.customtts.TTS
import com.guillermonegrete.tts.data.source.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * WordRepositorySource binding to use in tests.
 *
 * Hilt will inject a [FakeWordRepository] instead of a [WordRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TestApplicationModuleBinds {

    @Binds
    abstract fun bindWordRepo(repo: FakeWordRepository): WordRepositorySource

    @Binds
    abstract fun bindTTS(tts: FakeTTS): TTS
}