package com.guillermonegrete.tts.di

import android.app.Application
import com.guillermonegrete.tts.SpeakableApplication
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
        ScreenTextModule::class
    ])
interface ApplicationComponent: AndroidInjector<SpeakableApplication>{

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: Application): ApplicationComponent
    }
}
