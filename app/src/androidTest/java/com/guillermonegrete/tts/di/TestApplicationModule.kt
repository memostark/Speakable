package com.guillermonegrete.tts.di

import android.content.Context
import androidx.room.Room
import com.guillermonegrete.tts.customtts.FakeTTS
import com.guillermonegrete.tts.customtts.TTS
import com.guillermonegrete.tts.data.source.*
import com.guillermonegrete.tts.db.FilesDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

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

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FilesDatabaseModule::class]
)
object FileMemoryDatabaseModule {

    @Singleton
    @Provides
    fun provideFilesDatabase(@ApplicationContext context: Context): FilesDatabase {
        return Room.inMemoryDatabaseBuilder(context, FilesDatabase::class.java).build()
    }
}

/**
 * Replaces all the network server with the url of the MockWebServer.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object MockNetworkModule {

    @Provides
    fun provideTestBaseUrl() = "http://localhost:8081"

}
