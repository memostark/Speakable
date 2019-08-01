package com.guillermonegrete.tts.di

import android.content.Context
import com.guillermonegrete.tts.SpeakableApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, ApplicationModule::class])
interface ApplicationComponent: AndroidInjector<SpeakableApplication>{

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): ApplicationComponent
    }
}
