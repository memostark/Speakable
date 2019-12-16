package com.guillermonegrete.tts.di

import android.app.Application
import com.guillermonegrete.tts.SpeakableApplication
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ApplicationModule::class,
        MainModule::class,
        ProcessTextModule::class,
        ScreenTextModule::class,
        VisualizeTextModule::class,
        SavedWordsModule::class,
        ImportTextModule::class
    ])
interface ApplicationComponent: AndroidInjector<SpeakableApplication>{

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: Application): ApplicationComponent
    }

    fun inject(fragment: TextInfoDialog)

    fun inject(fragment: SaveWordDialogFragment)
}
