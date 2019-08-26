package com.guillermonegrete.tts.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Xml
import androidx.room.Room
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.guillermonegrete.tts.BuildConfig
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.data.source.*
import com.guillermonegrete.tts.data.source.local.AssetsExternalLinksSource
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource
import com.guillermonegrete.tts.data.source.remote.WiktionarySource
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.db.WordsDatabase
import com.guillermonegrete.tts.imageprocessing.*
import com.guillermonegrete.tts.main.TranslatorEnumKey
import com.guillermonegrete.tts.main.TranslatorType
import com.guillermonegrete.tts.threading.MainThreadImpl
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton
import dagger.Binds
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import org.xmlpull.v1.XmlPullParser


@Module(
    includes = [
        ApplicationModuleBinds::class,
        GoogleSourceModule::class,
        MicrosoftSourceModule::class,
        FirebaseLocalModule::class,
        FirebaseCloudModule::class
    ])
object ApplicationModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideAppContext(app: Application): Context = app.applicationContext

    @JvmStatic
    @Singleton
    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RemoteTranslationDataSource

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class WordsLocalDataSource

    @JvmStatic
    @Provides
    fun provideTranslatorEnum(preferences: SharedPreferences): TranslatorType{
        val value = TranslatorType.GOOGLE_PUBLIC.value
        val translatorPreference =
            Integer.parseInt(preferences.getString(TranslatorType.PREFERENCE_KEY, value.toString())!!)
        return TranslatorType.valueOf(translatorPreference)
    }

    @JvmStatic
    @Provides
    fun provideRecognizerEnum(preferences: SharedPreferences): TextRecognizerType{
        val defaultType = TextRecognizerType.FIREBASE_LOCAL.value
        val recognizerPreference =
            Integer.parseInt(preferences.getString(TextRecognizerType.PREFERENCE_KEY, defaultType.toString())!!)
        return TextRecognizerType.valueOf(recognizerPreference)
    }

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
    @Provides
    fun provideWordsDAO(database: WordsDatabase): WordsDAO = database.wordsDAO()

    @JvmStatic
    @RemoteTranslationDataSource
    @Provides
    fun provideRemoteTranslationSource(type: TranslatorType, map: @JvmSuppressWildcards Map<TranslatorType, WordDataSource>): WordDataSource{
        return map[type] ?: GooglePublicSource()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideExternalLinksSource(app: Application): ExternalLinksDataSource = AssetsExternalLinksSource(app)

    @JvmStatic
    @Singleton
    @Provides
    fun provideWiktionarySource(): DictionaryDataSource = WiktionarySource()

    @JvmStatic
    @Provides
    fun provideTextDetectorSource(
        type: TextRecognizerType,
        map: @JvmSuppressWildcards Map<TextRecognizerType, ImageProcessingSource>
    ): ImageProcessingSource {
        return map[type] ?: FirebaseTextProcessor()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @JvmStatic
    @Singleton
    @Provides
    fun provideXmlParser(): XmlPullParser = Xml.newPullParser()
}

@Module
abstract class ApplicationModuleBinds {

    @Binds
    abstract fun bindExecutor(executor: ThreadExecutor): Executor

    @Binds
    abstract fun bindThread(executor: MainThreadImpl): MainThread

    @Binds
    abstract fun bindRepository(repository: WordRepository): WordRepositorySource
}

/**
 * It's necessary to put every WordDataSource in their own module because
 * dagger can't determine how to create the instance if multiple methods return
 * the same type in the same module.
 */
@Module
class GoogleSourceModule{
    @Provides
    @Singleton
    @IntoMap
    @TranslatorEnumKey(TranslatorType.GOOGLE_PUBLIC)
    fun provideGooglePublicSource(): WordDataSource = GooglePublicSource()
}

@Module
class MicrosoftSourceModule{
    @Provides
    @Singleton
    @IntoMap
    @TranslatorEnumKey(TranslatorType.MICROSOFT)
    fun provideMicrosoftSource(): WordDataSource = MSTranslatorSource(BuildConfig.TranslatorApiKey)
}

@Module
class FirebaseLocalModule{
    @Provides
    @Singleton
    @IntoMap
    @TextRecognizerEnumKey(TextRecognizerType.FIREBASE_LOCAL)
    fun provideLocalTextRecognizer(): ImageProcessingSource = FirebaseTextProcessor()
}

@Module
class FirebaseCloudModule{
    @Provides
    @Singleton
    fun providesVisionRecognizer(): FirebaseVisionTextRecognizer = FirebaseVision.getInstance().cloudTextRecognizer

    @Provides
    @Singleton
    @IntoMap
    @TextRecognizerEnumKey(TextRecognizerType.FIREBASE_CLOUD)
    fun provideCloudTextRecognizer(visionRecognizer: FirebaseVisionTextRecognizer): ImageProcessingSource = FirebaseCloudTextProcessor(visionRecognizer)
}