package com.guillermonegrete.tts.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Xml
import androidx.room.Room
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.data.preferences.DefaultSettingsRepository
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.data.source.*
import com.guillermonegrete.tts.data.source.local.AssetsExternalLinksSource
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.data.source.remote.WiktionarySource
import com.guillermonegrete.tts.db.FileDAO
import com.guillermonegrete.tts.db.FilesDatabase
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.db.WordsDatabase
import com.guillermonegrete.tts.imageprocessing.*
import com.guillermonegrete.tts.importtext.visualize.io.DefaultEpubFileManager
import com.guillermonegrete.tts.importtext.visualize.io.EpubFileManager
import com.guillermonegrete.tts.main.TranslatorEnumKey
import com.guillermonegrete.tts.main.TranslatorType
import com.guillermonegrete.tts.threading.MainThreadImpl
import com.guillermonegrete.tts.ui.BrightnessTheme
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import org.xmlpull.v1.XmlPullParser

@InstallIn(SingletonComponent::class)
@Module
object ApplicationModule {

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RemoteTranslationDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class WordsLocalDataSource

    @Provides
    fun provideTranslatorEnum(preferences: SharedPreferences): TranslatorType{
        val value = TranslatorType.GOOGLE_PUBLIC.value
        val translatorPreference =
            Integer.parseInt(preferences.getString(TranslatorType.PREFERENCE_KEY, value.toString())!!)
        return TranslatorType.valueOf(translatorPreference)
    }

    @Provides
    fun provideRecognizerEnum(preferences: SharedPreferences): TextRecognizerType{
        val defaultType = TextRecognizerType.FIREBASE_LOCAL.value
        val recognizerPreference =
            Integer.parseInt(preferences.getString(TextRecognizerType.PREFERENCE_KEY, defaultType.toString())!!)
        return TextRecognizerType.valueOf(recognizerPreference)
    }

    @Provides
    fun provideBrightnessTheme(preferences: SharedPreferences): BrightnessTheme{
        val defaultType = BrightnessTheme.WHITE.value
        val recognizerPreference = preferences.getString(BrightnessTheme.PREFERENCE_KEY, defaultType) ?: defaultType
        return BrightnessTheme.get(recognizerPreference)
    }

    @Singleton
    @WordsLocalDataSource
    @Provides
    fun provideLocalSource(database: WordsDatabase): WordDataSource = WordLocalDataSource(database.wordsDAO())

    @Singleton
    @Provides
    fun provideWordsDatabase(@ApplicationContext context: Context): WordsDatabase{
        return Room.databaseBuilder(
            context,
            WordsDatabase::class.java,
            "words.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideWordsDAO(database: WordsDatabase): WordsDAO = database.wordsDAO()

    @Singleton
    @Provides
    fun provideFilesDatabase(@ApplicationContext context: Context): FilesDatabase{
        return FilesDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideFileDAO(database: FilesDatabase): FileDAO = database.fileDao()

    @RemoteTranslationDataSource
    @Provides
    fun provideRemoteTranslationSource(type: TranslatorType, map: @JvmSuppressWildcards Map<TranslatorType, WordDataSource>): WordDataSource{
        return map[type] ?: GooglePublicSource()
    }

    @Singleton
    @Provides
    fun provideExternalLinksSource(app: Application): ExternalLinksDataSource = AssetsExternalLinksSource(app)

    @Singleton
    @Provides
    fun provideWiktionarySource(): DictionaryDataSource = WiktionarySource()

    @Provides
    fun provideTextDetectorSource(
        type: TextRecognizerType,
        map: @JvmSuppressWildcards Map<TextRecognizerType, ImageProcessingSource>
    ): ImageProcessingSource {
        return map[type] ?: FirebaseTextProcessor()
    }

    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @Singleton
    @Provides
    fun provideXmlParser(): XmlPullParser = Xml.newPullParser()
}

@InstallIn(ApplicationComponent::class)
@Module
abstract class ApplicationModuleBinds {

    @Binds
    abstract fun bindExecutor(executor: ThreadExecutor): Executor

    @Binds
    abstract fun bindThread(executor: MainThreadImpl): MainThread

    @Binds
    abstract fun bindFileRepository(repository: DefaultFileRepository): FileRepository

    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindEpubFileManager(manager: DefaultEpubFileManager): EpubFileManager
}

/**
 * The binding for WordRepositorySource is on its own module so that we can replace it easily in tests.
 */
@InstallIn(ApplicationComponent::class)
@Module
abstract class WordRepositorySourceModule {
    @Binds
    abstract fun bindRepository(repository: WordRepository): WordRepositorySource
}

/**
 * It's necessary to put every WordDataSource in their own module because
 * dagger can't determine how to create the instance if multiple methods return
 * the same type in the same module.
 */
@InstallIn(ApplicationComponent::class)
@Module
class GoogleSourceModule{
    @Provides
    @Singleton
    @IntoMap
    @TranslatorEnumKey(TranslatorType.GOOGLE_PUBLIC)
    fun provideGooglePublicSource(): WordDataSource = GooglePublicSource()
}

@InstallIn(ApplicationComponent::class)
@Module
class FirebaseLocalModule{
    @Provides
    @Singleton
    @IntoMap
    @TextRecognizerEnumKey(TextRecognizerType.FIREBASE_LOCAL)
    fun provideLocalTextRecognizer(): ImageProcessingSource = FirebaseTextProcessor()
}